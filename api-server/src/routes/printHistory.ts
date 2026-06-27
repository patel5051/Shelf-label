import { Router, type IRouter } from "express";
import { ilike, or, sql, desc } from "drizzle-orm";
import { db, printHistoryTable } from "@workspace/db";
import {
  ListPrintHistoryQueryParams,
  CreatePrintHistoryEntryBody,
} from "@workspace/api-zod";

const router: IRouter = Router();

function formatEntry(row: typeof printHistoryTable.$inferSelect) {
  return {
    ...row,
    price: Number(row.price),
    printedAt: row.printedAt.toISOString(),
  };
}

router.get("/print-history", async (req, res): Promise<void> => {
  const parsed = ListPrintHistoryQueryParams.safeParse(req.query);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }

  const { search, page = 1, limit = 20 } = parsed.data;
  const offset = (page - 1) * limit;

  let baseQuery = db.select().from(printHistoryTable);
  let countQuery = db.select({ count: sql<number>`count(*)` }).from(printHistoryTable);

  if (search) {
    const condition = or(
      ilike(printHistoryTable.barcode, `%${search}%`),
      ilike(printHistoryTable.description, `%${search}%`),
    );
    baseQuery = baseQuery.where(condition) as typeof baseQuery;
    countQuery = countQuery.where(condition) as typeof countQuery;
  }

  const [entries, countResult] = await Promise.all([
    baseQuery.orderBy(desc(printHistoryTable.printedAt)).limit(limit).offset(offset),
    countQuery,
  ]);

  res.json({
    entries: entries.map(formatEntry),
    total: Number(countResult[0]?.count ?? 0),
    page,
    limit,
  });
});

router.post("/print-history", async (req, res): Promise<void> => {
  const parsed = CreatePrintHistoryEntryBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }

  const [entry] = await db
    .insert(printHistoryTable)
    .values({
      itemId: parsed.data.itemId ?? null,
      barcode: parsed.data.barcode,
      description: parsed.data.description,
      price: String(parsed.data.price),
      department: parsed.data.department,
      size: parsed.data.size,
    })
    .returning();

  res.status(201).json(formatEntry(entry));
});

export default router;

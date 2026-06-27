import { Router, type IRouter } from "express";
import { eq, ilike, or, sql, desc } from "drizzle-orm";
import { db, itemsTable } from "@workspace/db";
import {
  ListItemsQueryParams,
  CreateItemBody,
  GetItemParams,
  UpdateItemParams,
  UpdateItemBody,
  DeleteItemParams,
  BulkUpsertItemsBody,
} from "@workspace/api-zod";

const router: IRouter = Router();

function formatItem(row: typeof itemsTable.$inferSelect) {
  return {
    ...row,
    price: Number(row.price),
    createdAt: row.createdAt.toISOString(),
    updatedAt: row.updatedAt.toISOString(),
  };
}

router.get("/items", async (req, res): Promise<void> => {
  const parsed = ListItemsQueryParams.safeParse(req.query);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }

  const { search, department, page = 1, limit = 20 } = parsed.data;
  const offset = (page - 1) * limit;

  const conditions: ReturnType<typeof ilike>[] = [];
  if (search) {
    conditions.push(
      ilike(itemsTable.barcode, `%${search}%`),
      ilike(itemsTable.description, `%${search}%`),
    );
  }

  let baseQuery = db.select().from(itemsTable);
  let countQuery = db.select({ count: sql<number>`count(*)` }).from(itemsTable);

  if (search && department) {
    baseQuery = baseQuery.where(
      sql`(${or(...conditions)}) AND ${eq(itemsTable.department, department)}`,
    ) as typeof baseQuery;
    countQuery = countQuery.where(
      sql`(${or(...conditions)}) AND ${eq(itemsTable.department, department)}`,
    ) as typeof countQuery;
  } else if (search) {
    baseQuery = baseQuery.where(or(...conditions)) as typeof baseQuery;
    countQuery = countQuery.where(or(...conditions)) as typeof countQuery;
  } else if (department) {
    baseQuery = baseQuery.where(eq(itemsTable.department, department)) as typeof baseQuery;
    countQuery = countQuery.where(eq(itemsTable.department, department)) as typeof countQuery;
  }

  const [items, countResult] = await Promise.all([
    baseQuery.orderBy(desc(itemsTable.updatedAt)).limit(limit).offset(offset),
    countQuery,
  ]);

  res.json({
    items: items.map(formatItem),
    total: Number(countResult[0]?.count ?? 0),
    page,
    limit,
  });
});

router.post("/items", async (req, res): Promise<void> => {
  const parsed = CreateItemBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }

  const [item] = await db
    .insert(itemsTable)
    .values({
      barcode: parsed.data.barcode,
      description: parsed.data.description,
      price: String(parsed.data.price),
      department: parsed.data.department,
      size: parsed.data.size,
    })
    .returning();

  res.status(201).json(formatItem(item));
});

router.post("/items/bulk", async (req, res): Promise<void> => {
  const parsed = BulkUpsertItemsBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }

  let inserted = 0;
  let updated = 0;
  let failed = 0;
  const errors: string[] = [];

  for (const item of parsed.data.items) {
    try {
      const existing = await db
        .select()
        .from(itemsTable)
        .where(eq(itemsTable.barcode, item.barcode))
        .limit(1);

      if (existing.length > 0) {
        await db
          .update(itemsTable)
          .set({
            description: item.description,
            price: String(item.price),
            department: item.department,
            size: item.size,
          })
          .where(eq(itemsTable.barcode, item.barcode));
        updated++;
      } else {
        await db.insert(itemsTable).values({
          barcode: item.barcode,
          description: item.description,
          price: String(item.price),
          department: item.department,
          size: item.size,
        });
        inserted++;
      }
    } catch (err) {
      failed++;
      errors.push(`Barcode ${item.barcode}: ${err instanceof Error ? err.message : String(err)}`);
    }
  }

  res.json({ inserted, updated, failed, errors });
});

router.get("/items/:id", async (req, res): Promise<void> => {
  const params = GetItemParams.safeParse(req.params);
  if (!params.success) {
    res.status(400).json({ error: params.error.message });
    return;
  }

  const [item] = await db.select().from(itemsTable).where(eq(itemsTable.id, params.data.id));

  if (!item) {
    res.status(404).json({ error: "Item not found" });
    return;
  }

  res.json(formatItem(item));
});

router.patch("/items/:id", async (req, res): Promise<void> => {
  const params = UpdateItemParams.safeParse(req.params);
  if (!params.success) {
    res.status(400).json({ error: params.error.message });
    return;
  }

  const parsed = UpdateItemBody.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }

  const updateData: Partial<typeof itemsTable.$inferInsert> = {};
  if (parsed.data.barcode !== undefined) updateData.barcode = parsed.data.barcode;
  if (parsed.data.description !== undefined) updateData.description = parsed.data.description;
  if (parsed.data.price !== undefined) updateData.price = String(parsed.data.price);
  if (parsed.data.department !== undefined) updateData.department = parsed.data.department;
  if (parsed.data.size !== undefined) updateData.size = parsed.data.size;

  const [item] = await db
    .update(itemsTable)
    .set(updateData)
    .where(eq(itemsTable.id, params.data.id))
    .returning();

  if (!item) {
    res.status(404).json({ error: "Item not found" });
    return;
  }

  res.json(formatItem(item));
});

router.delete("/items/:id", async (req, res): Promise<void> => {
  const params = DeleteItemParams.safeParse(req.params);
  if (!params.success) {
    res.status(400).json({ error: params.error.message });
    return;
  }

  const [item] = await db
    .delete(itemsTable)
    .where(eq(itemsTable.id, params.data.id))
    .returning();

  if (!item) {
    res.status(404).json({ error: "Item not found" });
    return;
  }

  res.sendStatus(204);
});

export default router;

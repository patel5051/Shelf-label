import { Router, type IRouter } from "express";
import { sql, gte, desc } from "drizzle-orm";
import { db, itemsTable, printHistoryTable } from "@workspace/db";

const router: IRouter = Router();

router.get("/stats", async (_req, res): Promise<void> => {
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const startOfWeek = new Date(startOfToday);
  startOfWeek.setDate(startOfToday.getDate() - startOfToday.getDay());

  const [
    totalItemsResult,
    totalDepartmentsResult,
    printedTodayResult,
    printedThisWeekResult,
    recentPrints,
    itemsByDepartmentResult,
  ] = await Promise.all([
    db.select({ count: sql<number>`count(*)` }).from(itemsTable),
    db.select({ count: sql<number>`count(distinct ${itemsTable.department})` }).from(itemsTable),
    db
      .select({ count: sql<number>`count(*)` })
      .from(printHistoryTable)
      .where(gte(printHistoryTable.printedAt, startOfToday)),
    db
      .select({ count: sql<number>`count(*)` })
      .from(printHistoryTable)
      .where(gte(printHistoryTable.printedAt, startOfWeek)),
    db.select().from(printHistoryTable).orderBy(desc(printHistoryTable.printedAt)).limit(5),
    db
      .select({
        department: itemsTable.department,
        count: sql<number>`count(*)`,
      })
      .from(itemsTable)
      .groupBy(itemsTable.department)
      .orderBy(sql`count(*) desc`),
  ]);

  res.json({
    totalItems: Number(totalItemsResult[0]?.count ?? 0),
    totalDepartments: Number(totalDepartmentsResult[0]?.count ?? 0),
    printedToday: Number(printedTodayResult[0]?.count ?? 0),
    printedThisWeek: Number(printedThisWeekResult[0]?.count ?? 0),
    recentPrints: recentPrints.map((row) => ({
      ...row,
      price: Number(row.price),
      printedAt: row.printedAt.toISOString(),
    })),
    itemsByDepartment: itemsByDepartmentResult.map((row) => ({
      department: row.department,
      count: Number(row.count),
    })),
  });
});

router.get("/departments", async (_req, res): Promise<void> => {
  const result = await db
    .selectDistinct({ department: itemsTable.department })
    .from(itemsTable)
    .orderBy(itemsTable.department);

  res.json(result.map((r) => r.department));
});

export default router;

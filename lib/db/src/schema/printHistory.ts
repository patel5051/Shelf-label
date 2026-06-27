import { pgTable, text, serial, timestamp, numeric, integer } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod/v4";

export const printHistoryTable = pgTable("print_history", {
  id: serial("id").primaryKey(),
  itemId: integer("item_id"),
  barcode: text("barcode").notNull(),
  description: text("description").notNull(),
  price: numeric("price", { precision: 10, scale: 2 }).notNull(),
  department: text("department").notNull(),
  size: text("size").notNull().default(""),
  printedAt: timestamp("printed_at", { withTimezone: true }).notNull().defaultNow(),
});

export const insertPrintHistorySchema = createInsertSchema(printHistoryTable).omit({
  id: true,
  printedAt: true,
});

export type InsertPrintHistory = z.infer<typeof insertPrintHistorySchema>;
export type PrintHistory = typeof printHistoryTable.$inferSelect;

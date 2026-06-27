import { Router, type IRouter } from "express";
import healthRouter from "./health";
import itemsRouter from "./items";
import printHistoryRouter from "./printHistory";
import statsRouter from "./stats";

const router: IRouter = Router();

router.use(healthRouter);
router.use(itemsRouter);
router.use(printHistoryRouter);
router.use(statsRouter);

export default router;

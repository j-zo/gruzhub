import { User } from "../../../user/domain/User";
import { OrderStatus } from "./OrderStatus";

export interface OrderStatusChange {
  id: number;
  updatedAt: number;
  orderId: number;
  newStatus: OrderStatus;
  updatedBy: User;
  master?: User;
  comment?: string;
}

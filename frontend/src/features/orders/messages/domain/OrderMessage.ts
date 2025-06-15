import { UserRole } from "../../../user/domain/UserRole";
import { MessageFile } from "./MessageFile";

export interface OrderMessage {
  id: number;
  orderId: number;
  userId: number;
  userRole: UserRole;
  text: string;
  date: number;
  fileCode?: string;
  file?: MessageFile;
  isViewedByMaster: boolean;
  isViewedByDriver: boolean;
  isViewedByCustomer: boolean;
}

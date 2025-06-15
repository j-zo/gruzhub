import { User } from "../../../user/domain/User";
import { Auto } from "../../auto/domain/Auto";
import { Address } from "./Address";
import { OrderStatus } from "./OrderStatus";

export interface Order {
  id: number;
  guaranteeUuid: string;

  customerId?: number;
  masterId?: number;
  driverId?: number;

  autos: Auto[];

  description?: string;
  notes?: string;
  createdAt: number;
  updatedAt: number;
  status: OrderStatus;
  lastStatusUpdateTime: number;

  address: Address;

  isNeedEvacuator: boolean;
  isNeedMobileTeam: boolean;

  driver?: User;
  customer?: User;
  master?: User;

  declinedMastersIds: number[];

  urgency: string;
}

import { User } from "../../../user/domain/User";
import { Transport } from "@/features/common/transport/domain/Transport";
import { Address } from "./Address";
import { OrderStatus } from "./OrderStatus";

export interface Order {
  id: number;
  guaranteeUuid: string;

  customerId?: number;
  masterId?: number;
  driverId?: number;

  transports: Transport[];

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

import { Auto } from "../../../auto/domain/Auto";

export interface CreateOrderRequest {
  guaranteeUuid: string;

  customerId?: number;
  driverId?: number;

  driverName: string;
  driverPhone: string;

  autos: Auto[];

  regionId: number;
  description?: string;
  notes?: string;

  isNeedEvacuator?: boolean;
  isNeedMobileTeam?: boolean;

  urgency: string;
}

import { Transport } from "@/features/common/transport/domain/Transport";

export interface CreateOrderRequest {
  guaranteeUuid: string;

  customerId?: number;
  driverId?: number;

  driverName: string;
  driverPhone: string;

  transports: Transport[];

  regionId: number;
  description?: string;
  notes?: string;

  isNeedEvacuator?: boolean;
  isNeedMobileTeam?: boolean;

  urgency: string;
}

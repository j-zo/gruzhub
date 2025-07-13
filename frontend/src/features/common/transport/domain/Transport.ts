import { TransportType } from "./TransportType";

export interface Transport {
  id: number;
  
  customerId?: number;
  driverId?: number;

  brand?: string;
  model?: string;
  vin?: string;
  number?: string;

  type: TransportType;

  parkNumber: string;
  column?: string;
  year?: number;
}

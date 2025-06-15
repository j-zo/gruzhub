import { AutoType } from "./AutoType";

export interface Auto {
  id: number;
  
  customerId?: number;
  driverId?: number;

  brand?: string;
  model?: string;
  vin?: string;
  number?: string;

  type: AutoType;
}

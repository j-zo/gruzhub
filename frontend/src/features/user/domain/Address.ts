import { Region } from "@/features/regions/domain/Region";

export interface Address {
  id: number;
  region?: Region;
  city: string;
  street: string;
  latitude?: number;
  longtitude?: number;
  regionName?: string;
}

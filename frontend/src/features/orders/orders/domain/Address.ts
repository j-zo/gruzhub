import { Region } from "@/features/regions/domain/Region";

export interface Address {
  id: number;
  regionId: number;
  region: Region;
  city?: string;
  street?: string;
  latitude?: number;
  longtitude?: number;
}

import { UserRole } from "./UserRole";

export interface UpdateUser {
  id: number;
  role: UserRole;

  name: string;
  inn?: string;

  email?: string;
  phone: string;
  password?: string;

  tripRadiusKm?: number;

  regionId?: number;
  city?: string;
  street?: string;
}

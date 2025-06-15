import { UserRole } from "./UserRole";

export interface CreateUser {
  name: string;
  inn: string;
  role: UserRole;

  email: string;
  phone: string;
  password: string;

  tripRadiusKm?: number;

  regionId: number;
  city: string;
  street: string;
}

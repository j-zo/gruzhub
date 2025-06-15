import { Address } from "./Address";
import { UserRole } from "./UserRole";

export interface User {
  id: number;
  role: UserRole;

  email?: string;
  phone: string;

  balance: number;

  name: string;
  inn?: string;
  tripRadiusKm?: number;

  address?: Address;

  registrationDate: number;

  connectedTelegramChats: {
    chatUuid: string;
    telegramChatId: number;
    title?: string;
  }[];
}

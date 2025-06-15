export interface ConnectTelegramRequest {
  id: number;
  firstName?: string;
  lastName?: string;
  username?: string;
  photoUrl?: string;
  authDate?: number;
  hash: string;
}

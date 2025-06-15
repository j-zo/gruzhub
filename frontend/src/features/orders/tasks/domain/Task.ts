export interface Task {
  id: number;
  autoId: number;
  orderId: number;

  name: string;
  description?: string;
  price?: string;

  createdAt: number;
  updatedAt: number;
}

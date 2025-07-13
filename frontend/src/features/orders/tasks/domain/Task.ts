export interface Task {
  id: number;
  transportId: number;
  orderId: number;

  name: string;
  description?: string;
  price?: string;

  createdAt: number;
  updatedAt: number;
}

import { OrderStatus } from "./OrderStatus";

export class OrderStatusNamer {
  static getStatusName(orderStatus: OrderStatus): string {
    if (orderStatus === OrderStatus.CREATED) return "Новая заявка";
    if (orderStatus === OrderStatus.CALCULATING) return "Делается расчёт работ";
    if (orderStatus === OrderStatus.REVIEWING)
      return "Ожидает подтверждение заказчика";
    if (orderStatus === OrderStatus.ACCEPTED) return "Согласован заказчиком (в работе)";
    if (orderStatus === OrderStatus.COMPLETED) return "Завершен";
    if (orderStatus === OrderStatus.CANCELED) return "Отменён";

    throw new Error("Not allowed value");
  }

  static getColorForStatusName(orderStatus: OrderStatus): string {
    if (orderStatus === OrderStatus.CREATED) return "text-blue-700";
    if (orderStatus === OrderStatus.CALCULATING) return "text-yellow-600";
    if (orderStatus === OrderStatus.REVIEWING) return "text-cyan-600";
    if (orderStatus === OrderStatus.ACCEPTED) return "text-green-600";
    if (orderStatus === OrderStatus.COMPLETED) return "text-gray-700";
    if (orderStatus === OrderStatus.CANCELED) return "text-gray-500";

    throw new Error("Not allowed value");
  }
}

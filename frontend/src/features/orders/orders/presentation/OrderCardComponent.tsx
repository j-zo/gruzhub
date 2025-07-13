import moment from "moment";
import "moment/locale/ru";
import { Order } from "../domain/Order";
import { OrderStatusNamer } from "../domain/OrderStatusNamer";
import { TransportType } from "@/features/common/transport/domain/TransportType";
import { Button } from "@mantine/core";

interface Props {
  order: Order;
  openOrder(): void;
}

export const OrderCardComponent = ({
  order,
  openOrder,
}: Props): JSX.Element => {
  const truck = order.transports.find((transport) => transport.type === TransportType.TRUCK);
  const trailer = order.transports.find((transport) => transport.type === TransportType.TRAILER);

  return (
    <div className="lg:mr-0 mb-5 text-xs p-5 border rounded-xl shadow whitespace-nowrap w-full flex flex-col">
      <div className="mb-3 font-bold text-sm">Заказ #{order.id}</div>

      <div className="flex max-w-[280px] overflow-x-hidden">
        <div className="w-[75px] min-w-[75px]">Размещен:</div>{" "}
        {moment(order.createdAt).format("lll")} (
        {moment(order.createdAt).fromNow()})
      </div>

      <div className="flex max-w-[280px] overflow-x-hidden">
        <div className="w-[75px] min-w-[75px]">Статус:</div>{" "}
        <span
          className={`font-bold ${OrderStatusNamer.getColorForStatusName(
            order.status
          )}`}
        >
          {OrderStatusNamer.getStatusName(order.status)}
        </span>
      </div>

      <div className="flex max-w-[280px] overflow-x-hidden">
        <div className="w-[75px] min-w-[75px]">Регион:</div>{" "}
        {order.address.region.name}
      </div>

      <div className="flex mt-3" />

      {truck && (
        <>
          <div className="flex max-w-[280px] overflow-x-hidden">
            <div className="w-[75px] min-w-[75px]">Грузовик:</div>
            {`${truck.brand ? `${truck.brand} ` : ""} ${truck.model}`}
          </div>
        </>
      )}

      {trailer && (
        <>
          <div className="flex max-w-[280px] overflow-x-hidden">
            <div className="w-[75px] min-w-[75px]">Прицеп:</div>
            {`${trailer.brand ? `${trailer.brand} ` : ""}${trailer.model}`}
          </div>
        </>
      )}

      {(order.isNeedEvacuator || order.isNeedMobileTeam) && (
        <>
          <div className="mt-3" />
          <div>{order.isNeedEvacuator && "Требуется эвакуатор"}</div>
          <div>{order.isNeedMobileTeam && "Требуется выездная бригада"}</div>
        </>
      )}

      <div className="mt-3" />

      <Button
        fullWidth
        size="xs"
        className="mt-auto"
        variant="outline"
        onClick={() => openOrder()}
      >
        См. детали
      </Button>
    </div>
  );
};

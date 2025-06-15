import moment from "moment";
import "moment/locale/ru";
import { User } from "../../../../user/domain/User";
import { OrderApiRepository } from "../../data/OrderApiRepository";
import { useEffect, useState } from "react";
import LoadingComponent from "../../../../../util/components/LoadingComponent";
import { UserRole } from "../../../../user/domain/UserRole";
import { OrderStatusChange } from "../../domain/OrderStatusChange";
import { Timeline } from "@mantine/core";
import { OrderStatus } from "../../domain/OrderStatus";
import { OrderStatusNamer } from "../../domain/OrderStatusNamer";
import { UserDetailsComponent } from "./UserDetailsComponent";
import StringUtils from "../../../../../util/StringUtils";

interface Props {
  orderApiRepository: OrderApiRepository;

  orderId: number;
  user: User;
}

export const OrderTimelineComponent = ({
  orderApiRepository,
  orderId,
  user,
}: Props): JSX.Element => {
  const [showingUser, setShowingUser] = useState<User | undefined>();

  const [isStatusesLoaded, setStatusesLoaded] = useState(false);
  const [statusChanges, setStatusChanges] = useState<OrderStatusChange[]>([]);

  useEffect(() => {
    setStatusChanges([]);
    setStatusesLoaded(false);

    let isRequestInProgress = false;

    const checkOrderStatusChangesInterval = setInterval(async () => {
      if (!isRequestInProgress) {
        isRequestInProgress = true;
        let isCleanInterval = false;

        try {
          const statusChanges = await orderApiRepository.getOrderStatusChanges(
            orderId
          );
          setStatusChanges(statusChanges);
          setStatusesLoaded(true);
        } catch (e) {
          isCleanInterval = true;
        }

        if (isCleanInterval && checkOrderStatusChangesInterval)
          clearInterval(checkOrderStatusChangesInterval);

        isRequestInProgress = false;
      }
    }, 1000);

    return () => {
      if (checkOrderStatusChangesInterval)
        clearInterval(checkOrderStatusChangesInterval);
    };
  }, [orderId]);

  let userTitle = "";
  if (user.role === UserRole.DRIVER) userTitle = "водителя";
  if (user.role === UserRole.MASTER) userTitle = "автосервиса";
  if (user.role === UserRole.CUSTOMER) userTitle = "заказчика";

  return (
    <>
      {isStatusesLoaded ? (
        <>
          {statusChanges.length > 0 && (
            <Timeline
              active={statusChanges.length - 1}
              bulletSize={12}
              lineWidth={2}
            >
              {statusChanges.map((statusChange) => {
                return (
                  <Timeline.Item
                    key={`status_change_${statusChange.id}`}
                    title={
                      statusChange.newStatus === OrderStatus.CREATED
                        ? "Поиск автосервиса"
                        : OrderStatusNamer.getStatusName(statusChange.newStatus)
                    }
                  >
                    <div className="text-xs">
                      <div>
                        {moment(statusChange.updatedAt).format("LLL")} (
                        {moment(statusChange.updatedAt).fromNow()})
                      </div>

                      {statusChange.master && (
                        <div>
                          Исполнитель (СТО):{" "}
                          <span
                            onClick={() => setShowingUser(statusChange.master)}
                            className="text-xs text-blue-600 cursor-pointer"
                          >
                            {StringUtils.cutBySymbolsLength(
                              statusChange.master.name,
                              22
                            )}
                          </span>
                        </div>
                      )}

                      {(statusChange.newStatus == OrderStatus.CANCELED ||
                        statusChange.newStatus == OrderStatus.COMPLETED) && (
                        <div>
                          Поменял статус:{" "}
                          <span
                            onClick={() => {
                              if (
                                statusChange.updatedBy.role === UserRole.ADMIN
                              )
                                return;

                              setShowingUser(statusChange.updatedBy);
                            }}
                            className={`text-xs ${
                              statusChange.updatedBy.role === UserRole.ADMIN
                                ? ""
                                : "text-blue-600 cursor-pointer"
                            }`}
                          >
                            {StringUtils.cutBySymbolsLength(
                              statusChange.updatedBy.name,
                              22
                            )}{" "}
                            (
                            {statusChange.updatedBy.role === UserRole.ADMIN &&
                              "администратор"}
                            {statusChange.updatedBy.role ===
                              UserRole.CUSTOMER && "заказчик"}
                            {statusChange.updatedBy.role === UserRole.MASTER &&
                              "СТО"}
                            {statusChange.updatedBy.role === UserRole.DRIVER &&
                              "водитель"}
                            )
                          </span>
                        </div>
                      )}

                      <div className="text-xs text-gray-400 mt-2 whitespace-pre">
                        {statusChange.comment}
                      </div>
                    </div>
                  </Timeline.Item>
                );
              })}

              {!statusChanges.find(
                (status) => status.newStatus === OrderStatus.CANCELED
              ) && (
                <>
                  {statusChanges[statusChanges.length - 1].newStatus ===
                    OrderStatus.CREATED && (
                    <Timeline.Item
                      className="!text-gray-400"
                      title={OrderStatusNamer.getStatusName(
                        OrderStatus.CALCULATING
                      )}
                    />
                  )}

                  {!statusChanges.find(
                    (status) => status.newStatus === OrderStatus.REVIEWING
                  ) && (
                    <Timeline.Item
                      className="!text-gray-400"
                      title={OrderStatusNamer.getStatusName(
                        OrderStatus.REVIEWING
                      )}
                    />
                  )}

                  {!statusChanges.find(
                    (status) => status.newStatus === OrderStatus.ACCEPTED
                  ) && (
                    <Timeline.Item
                      className="!text-gray-400"
                      title={OrderStatusNamer.getStatusName(
                        OrderStatus.ACCEPTED
                      )}
                    />
                  )}

                  {!statusChanges.find(
                    (status) => status.newStatus === OrderStatus.COMPLETED
                  ) && (
                    <Timeline.Item
                      className="!text-gray-400"
                      title={OrderStatusNamer.getStatusName(
                        OrderStatus.COMPLETED
                      )}
                    />
                  )}
                </>
              )}
            </Timeline>
          )}
        </>
      ) : (
        <LoadingComponent />
      )}

      {showingUser && (
        <UserDetailsComponent
          orderApiRepository={orderApiRepository}
          orderId={orderId}
          user={showingUser}
          close={() => setShowingUser(undefined)}
          isLoadStatusChanges={false}
        />
      )}
    </>
  );
};

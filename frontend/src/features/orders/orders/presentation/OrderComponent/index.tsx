import moment from "moment";
import "moment/locale/ru";
import { Button } from "@mantine/core";
import { OrderApiRepository } from "../../data/OrderApiRepository";
import { useEffect, useState } from "react";
import { Order } from "../../domain/Order";
import LoadingComponent from "../../../../../util/components/LoadingComponent";
import { OrderStatusNamer } from "../../domain/OrderStatusNamer";
import { TransportType } from "@/features/common/transport/domain/TransportType";
import { User } from "../../../../user/domain/User";
import UserApiRepository from "../../../../user/data/UserApiRepository";
import { UserRole } from "../../../../user/domain/UserRole";
import { OrderStatus } from "../../domain/OrderStatus";
import { OrderTasksComponent } from "../../../tasks/presentation/OrderTasksComponent";
import { TaskApiRepository } from "@/features/orders/tasks/data/TaskApiRepository";
import { OrderTransportComponent } from "@/features/common/transport/presentation/OrderTransportComponent";
import { TransportApiRepository } from "@/features/common/transport/data/TransportApiRepository";
import { ConfirmationComponent } from "../../../../../util/components/ConfirmationComponent";
import { UserDetailsComponent } from "./UserDetailsComponent";
import { OrderTimelineComponent } from "./OrderTimelineComponent";
import Image from "next/image";
import { useScreenWidth } from "../../../../../util/hook/useScreenWidth";
import { OrdersChatComponent } from "../../../messages/presentation/OrdersChatComponent";
import { OrderMessageApiRepository } from "../../../messages/data/OrderMessageApiRepository";
import { SidebarComponent } from "../../../../../util/components/SidebarComponent";
import styles from "./index.module.css";
import { ORDER_PRICE } from "@/constants";
import StringUtils from "@/util/StringUtils";

interface Props {
  orderMessageApiRepository: OrderMessageApiRepository;
  orderApiRepository: OrderApiRepository;
  userApiRepository: UserApiRepository;
  taskApiRepository: TaskApiRepository;
  transportApiRepository: TransportApiRepository;

  orderId: number;

  user: User;

  isSidebar: boolean;
  onClose?(): void;
}

export const OrderComponent = ({
  orderMessageApiRepository,
  orderApiRepository,
  userApiRepository,
  taskApiRepository, transportApiRepository,
  orderId,

  user,
  isSidebar,
  onClose,
}: Props): JSX.Element => {
  const screenWidth = useScreenWidth();
  const [isShowMobileChats, setShowMobileChats] = useState(false);

  const [order, setOrder] = useState<Order | undefined>();
  const [orderLoadError, setOrderLoadError] = useState("");

  const [isDecliningMaster, setDecliningMaster] = useState(false);
  const [showDeclineMasterConfirmation, setShowDeclineConfirmation] =
    useState(false);

  const [isSendingForConfirmation, setSendingForConfirmation] = useState(false);
  const [showSendForConfrimationDialog, setSendForConfirmationDialog] =
    useState(false);

  const [isAcceping, setAccepting] = useState(false);

  const [isCompleting, setCompleting] = useState(false);
  const [showCompleteConfirmation, setCompleteConfirmation] = useState(false);

  const [isCancelling, setCancelling] = useState(false);
  const [isShowCancelConfirmation, setShowCancelConfirmation] = useState(false);

  const [isShowDriverDetails, setShowDriverDetails] = useState(false);

  const [isSidebarHidden, setSidebarHidden] = useState(false);

  const startCalculationByMaster = async () => {
    try {
      await orderApiRepository.startCalculationByMaster(orderId);
      userApiRepository.notifyAuthListeners();
    } catch (e) {
      alert(
        "Не вышло взять заказ в работу. Возможная причина: кто-то другой только что взял заказ в работу"
      );
      console.log((e as Error).message);
    }
  };

  const loadOrder = async () => {
    const order = await orderApiRepository.getOrder(orderId);
    setOrder(order);
  };

  const declineMaster = async (reason?: string) => {
    if (!reason) throw new Error("NPE");

    setShowDeclineConfirmation(false);
    setDecliningMaster(true);

    try {
      await orderApiRepository.declineMasterByCustomer(orderId, reason);
      await loadOrder();
    } catch (e) {
      console.log((e as Error).message);
      alert("Ошибка при смене автосервиса. Пожалуйста, перезагрузите страницу");
    }

    setDecliningMaster(false);
  };

  const cancelOrder = async (reason?: string) => {
    if (!reason) throw new Error("NPE");

    setShowCancelConfirmation(false);
    setCancelling(true);

    try {
      await orderApiRepository.cancelOrder(orderId, reason);
      await loadOrder();
    } catch (e) {
      console.log((e as Error).message);
      alert("Ошибка при отмене заказа. Пожалуйста, перезагрузите страницу");
    }

    setCancelling(false);
  };

  const sendForConfrimation = async () => {
    setSendForConfirmationDialog(false);
    setSendingForConfirmation(true);

    try {
      await orderApiRepository.sendForConfirmationByMaster(orderId);
      await loadOrder();
    } catch (e) {
      console.log((e as Error).message);
      alert(
        "Ошибка при отправке на согласование. Пожалуйста, перезагрузите страницу"
      );
    }

    setSendingForConfirmation(false);
  };

  const accept = async () => {
    setAccepting(true);

    try {
      await orderApiRepository.acceptByCustomer(orderId);
      await loadOrder();
    } catch (e) {
      console.log((e as Error).message);
      alert(
        "Ошибка при согласовании заказа. Пожалуйста, перезагрузите страницу"
      );
    }

    setAccepting(false);
  };

  const complete = async () => {
    setCompleting(true);
    setCompleteConfirmation(false);

    try {
      await orderApiRepository.completeOrder(orderId);
      await loadOrder();
    } catch (e) {
      console.log((e as Error).message);
      alert("Ошибка при завершении заказа. Пожалуйста, перезагрузите страницу");
    }

    setCompleting(false);
  };

  useEffect(() => {
    setOrder(undefined);
    let isRequestInProgress = false;

    const checkOrderAvailableInterval = setInterval(async () => {
      if (!isRequestInProgress) {
        isRequestInProgress = true;
        let isCleanInterval = false;

        try {
          await loadOrder();
        } catch (e) {
          setOrderLoadError((e as Error).message);
          isCleanInterval = true;
        }

        if (isCleanInterval && checkOrderAvailableInterval)
          clearInterval(checkOrderAvailableInterval);

        isRequestInProgress = false;
      }
    }, 1000);

    return () => {
      if (checkOrderAvailableInterval)
        clearInterval(checkOrderAvailableInterval);
    };
  }, [orderId]);

  if (order) {
    order.transports.sort((a1, a2) => {
      if (a1.type === TransportType.TRUCK && a2.type === TransportType.TRUCK) return 1;
      if (a1.type === TransportType.TRUCK && a2.type === TransportType.TRAILER) return -1;
      return 0;
    });
  }

  return (
    <div className="flex">
      {screenWidth && screenWidth >= 1280 && (
        <>
          <div className="max-w-[320px] min-w-[320px]" />

          <div
            className="max-w-[320px] min-w-[320px] hidden xl:block"
            style={{ position: "fixed" }}
          >
            <div className="border-l border-r">
              <OrdersChatComponent
                isFullHeight
                setSidebarHidden={setSidebarHidden}
                user={user}
                orderMessageApiRepository={orderMessageApiRepository}
                ordersIds={
                  !order ||
                  (order.status === OrderStatus.CREATED &&
                    user.role === UserRole.MASTER)
                    ? []
                    : [orderId]
                }
                isParentLoading={false}
                isSingleChat
                isChatClosed={
                  order?.status === OrderStatus.CREATED ||
                  order?.status === OrderStatus.CANCELED ||
                  order?.status === OrderStatus.COMPLETED
                }
              />
            </div>
          </div>
        </>
      )}

      {!isSidebarHidden && (
        <div
          className={`grow ${isSidebar ? "p-5" : ""} ${
            styles.mobileScrollFullHeight
          }`}
        >
          {isSidebar && (
            <div className="flex justify-between items-center">
              <div className="text-xl font-bold">Заказ #{orderId}</div>

              <Image
                style={{
                  cursor: "pointer",
                  filter:
                    "invert(21%) sepia(89%) saturate(4349%) hue-rotate(226deg) brightness(89%) contrast(90%)",
                }}
                onClick={() => {
                  if (onClose) onClose();
                }}
                width={30}
                height={30}
                src="/icons/close.svg"
                alt="close"
              />
            </div>
          )}

          {orderLoadError ? (
            <div>Заказ взят в работу другим исполнителем</div>
          ) : (
            <div>
              {order ? (
                <div>
                  <div className="lg:flex">
                    <div className="w-full lg:w-2/4 pr-10">
                      <div className="flex text-sm">
                        <div className="w-[100px] min-w-[100px]">Размещен:</div>{" "}
                        {moment(order.createdAt).format("lll")} (
                        {moment(order.createdAt).fromNow()})
                      </div>

                      <div className="flex text-sm">
                        <div className="w-[100px] min-w-[100px]">Статус:</div>{" "}
                        <span
                          className={`font-bold ${OrderStatusNamer.getColorForStatusName(
                            order.status
                          )}`}
                        >
                          {OrderStatusNamer.getStatusName(order.status)}
                        </span>
                      </div>

                      <div className="flex text-sm">
                        <div className="w-[100px] min-w-[100px]">Регион:</div>{" "}
                        {order.address.region.name}
                      </div>

                      <div className="flex text-sm">
                        <div className="w-[100px] min-w-[100px]">
                          Срочность:
                        </div>{" "}
                        {order.urgency}
                      </div>

                      {user.balance >= ORDER_PRICE &&
                        user.role === UserRole.MASTER &&
                        order.status === OrderStatus.CREATED && (
                          <Button
                            className="mt-3 mr-1"
                            onClick={() => startCalculationByMaster()}
                          >
                            Взять в работу (открыть данные)
                          </Button>
                        )}

                      {user.balance < ORDER_PRICE &&
                        order.status === OrderStatus.CREATED &&
                        user.role === UserRole.MASTER && (
                          <Button
                            className="mt-3 mr-1"
                            onClick={() => {
                              window.location.href = "/increase-balance";
                            }}
                            color="red"
                          >
                            Недостаточно средств, чтобы взять в работу
                          </Button>
                        )}

                      {user.role === UserRole.MASTER &&
                        order.status === OrderStatus.CALCULATING && (
                          <Button
                            className="mt-3 mr-1"
                            onClick={() => setSendForConfirmationDialog(true)}
                            disabled={isSendingForConfirmation}
                          >
                            Отправить на согласование
                          </Button>
                        )}

                      {(user.role === UserRole.CUSTOMER ||
                        user.role === UserRole.DRIVER) &&
                        order.status === OrderStatus.REVIEWING && (
                          <Button
                            className="mt-3 mr-1"
                            onClick={() => accept()}
                            disabled={isAcceping}
                          >
                            Согласовать предложенные работы
                          </Button>
                        )}

                      {order.status === OrderStatus.ACCEPTED && (
                        <Button
                          className="mt-3 mr-1"
                          onClick={() => setCompleteConfirmation(true)}
                          disabled={isCompleting}
                        >
                          Завершить заказ
                        </Button>
                      )}

                      {user.role === UserRole.MASTER &&
                        (order.status === OrderStatus.REVIEWING ||
                          order.status === OrderStatus.CALCULATING) && (
                          <Button
                            className="mt-3 mr-1"
                            onClick={() => setCompleteConfirmation(true)}
                            disabled={isCompleting}
                            variant="outline"
                          >
                            Завершить заказ
                          </Button>
                        )}
                    </div>

                    {screenWidth && screenWidth < 1280 && (
                      <div className="my-3">
                        <Button
                          onClick={() => setShowMobileChats(true)}
                          fullWidth
                        >
                          Открыть чат заказа
                        </Button>
                      </div>
                    )}

                    <div className="mt-5 lg:mt-0 w-full lg:w-2/4 pr-10">
                      <OrderTimelineComponent
                        orderApiRepository={orderApiRepository}
                        orderId={orderId}
                        user={user}
                      />
                    </div>
                  </div>

                  <div className="mt-5" />

                  <div className="lg:flex">
                    {(user.role !== UserRole.MASTER ||
                      order.status !== OrderStatus.CREATED ||
                      order.masterId === user.id) && (
                      <div className="w-full lg:w-2/4 pr-10">
                        {order.customer && (
                          <div>
                            <div className="font-bold mb-3">Перевозчик</div>

                            <div className="flex text-sm max-w-full overflow-x-hidden">
                              <div className="w-[100px] min-w-[100px]">
                                Название:
                              </div>{" "}
                              {order.customer.name}
                            </div>

                            <div className="flex text-sm max-w-full overflow-x-hidden">
                              <div className="w-[100px] min-w-[100px]">
                                ИНН:
                              </div>{" "}
                              {order.customer.inn}
                            </div>

                            <div className="flex text-sm max-w-full overflow-x-hidden">
                              <div className="w-[100px] min-w-[100px]">
                                Номер:
                              </div>{" "}
                              {order.customer.phone}
                            </div>
                          </div>
                        )}

                        {order.driver && (
                          <div>
                            {order.customer && <div className="mt-5" />}

                            <div className="font-bold mb-3">Водитель</div>

                            <div className="flex text-sm max-w-full overflow-x-hidden">
                              <div className="w-[100px] min-w-[100px]">
                                Имя:
                              </div>{" "}
                              {order.driver.name}
                            </div>

                            <div className="flex text-sm max-w-full overflow-x-hidden">
                              <div className="w-[100px] min-w-[100px]">
                                Телефон:
                              </div>{" "}
                              {StringUtils.formatAsPhone(order.driver.phone)}
                            </div>

                            <Button
                              onClick={() => {
                                setShowDriverDetails(true);
                              }}
                              className="mt-2 mr-1"
                              size="xs"
                              variant="outline"
                            >
                              Детальнее
                            </Button>

                            <Button
                              onClick={() => {
                                window.location.href = `tel:+${order?.driver?.phone}`;
                              }}
                              className="mt-2"
                              size="xs"
                              variant="outline"
                            >
                              Позвонить водителю
                            </Button>
                          </div>
                        )}
                      </div>
                    )}

                    <div className="w-full lg:w-2/4 pr-10 mt-5 lg:mt-0">
                      {order.master && (
                        <div>
                          <div className="font-bold mb-3">Автосервис</div>

                          {user.role === UserRole.ADMIN && (
                            <div className="flex text-sm max-w-full overflow-x-hidden">
                              <div className="w-[100px] min-w-[100px]">ID:</div>{" "}
                              #{order.master.id}
                            </div>
                          )}

                          {user.role === UserRole.ADMIN && (
                            <div className="flex text-sm max-w-full overflow-x-hidden">
                              <div className="w-[100px] min-w-[100px]">
                                Баланс:
                              </div>{" "}
                              {order.master.balance.toLocaleString()} руб
                            </div>
                          )}

                          <div className="flex text-sm max-w-full overflow-x-hidden">
                            <div className="w-[100px] min-w-[100px]">
                              Название:
                            </div>{" "}
                            {order.master.name}
                          </div>

                          <div className="flex text-sm max-w-full overflow-x-hidden">
                            <div className="w-[100px] min-w-[100px]">
                              Телефон:
                            </div>{" "}
                            {StringUtils.formatAsPhone(order.master.phone)}
                          </div>

                          <Button
                            onClick={() => {
                              window.location.href = `tel:+${order?.master?.phone}`;
                            }}
                            className="mt-2"
                            size="xs"
                            variant="outline"
                          >
                            Позвонить СТО
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="mt-5" />

                  <div className="lg:flex">
                    <div className="w-full lg:w-2/4 pr-10">
                      <div className="mt-5 lg:mt-0" />

                      <OrderTransportComponent
                        transportApiRepository={transportApiRepository}
                        order={order}
                        transportId={order.transports[0].id}
                        user={user}
                      />

                      {order.transports.length === 2 && (
                        <>
                          <div className="mt-5" />

                          <OrderTransportComponent
                              transportApiRepository={transportApiRepository}
                            order={order}
                            transportId={order.transports[1].id}
                            user={user}
                          />
                        </>
                      )}

                      {(order.isNeedEvacuator || order.isNeedMobileTeam) && (
                        <>
                          <div className="mt-5" />

                          <div className="font-bold mb-3">Доп. требования</div>

                          <div className="text-sm">
                            {order.isNeedEvacuator && "- Требуется эвакуатор"}
                          </div>
                          <div className="text-sm">
                            {order.isNeedMobileTeam &&
                              "- Требуется выездная бригада"}
                          </div>
                        </>
                      )}
                    </div>

                    {order.description && (
                      <div className="w-full lg:w-2/4 pr-10">
                        <div className="font-bold mb-3">Описание проблемы</div>
                        <div className="whitespace-pre-line text-sm">
                          {order.description}
                        </div>

                        {order.notes && (
                          <>
                            <div className="font-bold mt-3 mb-3">
                              Примечания
                            </div>
                            <div className="whitespace-pre-line text-sm">
                              {order.notes}
                            </div>
                          </>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="mt-5" />

                  {order.status !== OrderStatus.CREATED && (
                    <OrderTasksComponent
                      taskApiRepository={taskApiRepository}
                      order={order}
                      user={user}
                    />
                  )}

                  <div className="mt-5" />

                  <div className="flex flex-wrap">
                    {(user.role === UserRole.CUSTOMER ||
                      user.role === UserRole.DRIVER ||
                      user.role === UserRole.ADMIN) &&
                      order.status !== OrderStatus.CREATED &&
                      order.status !== OrderStatus.CANCELED &&
                      order.status !== OrderStatus.COMPLETED && (
                        <Button
                          className="mt-3 mr-2"
                          onClick={() => setShowDeclineConfirmation(true)}
                          variant="outline"
                          color="red"
                          disabled={isDecliningMaster}
                        >
                          Сменить автосервис
                        </Button>
                      )}

                    {(user.role === UserRole.ADMIN ||
                      user.role === UserRole.CUSTOMER ||
                      user.role === UserRole.DRIVER) &&
                      (order.status !== OrderStatus.ACCEPTED ||
                        user.role === UserRole.ADMIN) &&
                      order.status !== OrderStatus.COMPLETED &&
                      order.status !== OrderStatus.CANCELED && (
                        <Button
                          disabled={isCancelling}
                          className="mt-3"
                          onClick={() => setShowCancelConfirmation(true)}
                          color="red"
                          variant="outline"
                        >
                          Отменить заказ
                        </Button>
                      )}
                  </div>

                  <div className="mt-5" />
                </div>
              ) : (
                <div className="flex justify-center">
                  <LoadingComponent />
                </div>
              )}
            </div>
          )}

          {showDeclineMasterConfirmation && (
            <ConfirmationComponent
              onConfirm={(reason) => declineMaster(reason)}
              onDecline={() => setShowDeclineConfirmation(false)}
              actionButtonColor="red"
              description="Вы уверены, что хотите сменить автосервис? Текущий сервис больше не сможет взять ваш заказ"
              actionText="Сменить"
              reasons={[
                "СТО не может принять в ближайшее время",
                "СТО не соответствует требованиям",
              ]}
            />
          )}

          {showSendForConfrimationDialog && (
            <ConfirmationComponent
              onConfirm={() => sendForConfrimation()}
              onDecline={() => setSendForConfirmationDialog(false)}
              actionButtonColor="blue"
              description="Вы уверены, что хотите отправить предложенные работы на согласование заказчику?"
              actionText="Отправить"
            />
          )}

          {showCompleteConfirmation && (
            <ConfirmationComponent
              onConfirm={() => complete()}
              onDecline={() => setCompleteConfirmation(false)}
              actionButtonColor="blue"
              description="Вы уверены, что хотите отметить заказ как завершенный?"
              actionText="Завершить заказ"
            />
          )}

          {isShowCancelConfirmation && (
            <ConfirmationComponent
              onConfirm={(reason) => cancelOrder(reason)}
              onDecline={() => setShowCancelConfirmation(false)}
              actionButtonColor="red"
              description="Вы уверены, что хотите отменить заказ?"
              actionText="Отменить заказ"
              reasons={
                user.role === UserRole.ADMIN
                  ? [
                      "Водитель / заказчик не выходит на связь",
                      "Водитель / заказчик решил проблему самостоятельно",
                      "Водитель / заказчик нашёл СТО самостоятельно",
                    ]
                  : [
                      "Решил проблему самостоятельно",
                      "Нашёл СТО самостоятельно",
                    ]
              }
            />
          )}

          {isShowDriverDetails && order?.driver && (
            <UserDetailsComponent
              orderApiRepository={orderApiRepository}
              orderId={order.id}
              user={order.driver}
              close={() => setShowDriverDetails(false)}
              isLoadStatusChanges={true}
            />
          )}

          {isShowMobileChats && user && (
            <SidebarComponent
              isFullScreen
              onCloseSidebar={() => setShowMobileChats(false)}
              sidebarName={`Чаты`}
              sidebarInner={
                <OrdersChatComponent
                  isShowCloseChatsButton
                  onCloseChats={() => setShowMobileChats(false)}
                  setSidebarHidden={setSidebarHidden}
                  user={user}
                  orderMessageApiRepository={orderMessageApiRepository}
                  ordersIds={[orderId]}
                  isParentLoading={false}
                  isSingleChat
                  isChatClosed={
                    order?.status === OrderStatus.CREATED ||
                    order?.status === OrderStatus.CANCELED ||
                    order?.status === OrderStatus.COMPLETED
                  }
                />
              }
            />
          )}
        </div>
      )}
    </div>
  );
};

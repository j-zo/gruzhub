import { Button, Modal, MultiSelect, Select } from "@mantine/core";
import { useState, useEffect } from "react";
import LoadingComponent from "../../../../util/components/LoadingComponent";
import { Order } from "../domain/Order";
import { OrderApiRepository } from "../data/OrderApiRepository";
import UserApiRepository from "../../../user/data/UserApiRepository";
import { OrderCardComponent } from "./OrderCardComponent";
import { CreateOrderComponent } from "./CreateOrderComponent";
import { OrderComponent } from "./OrderComponent";
import { User } from "../../../user/domain/User";
import { UserRole } from "../../../user/domain/UserRole";
import { TaskApiRepository } from "@/features/orders/tasks/data/TaskApiRepository";
import { TransportApiRepository } from "@/features/common/transport/data/TransportApiRepository";
import { OrderStatusNamer } from "../domain/OrderStatusNamer";
import { OrderStatus } from "../domain/OrderStatus";
import { useRegions } from "../../../regions/hooks/useRegions";
import moment from "moment";
import { TransportType } from "@/features/common/transport/domain/TransportType";
import { SidebarComponent } from "../../../../util/components/SidebarComponent";
import { VisiblityDetectorComponent } from "../../../../util/components/visiblity/VisiblityDetectorComponent";
import { OrdersChatComponent } from "../../messages/presentation/OrdersChatComponent";
import { OrderMessageApiRepository } from "../../messages/data/OrderMessageApiRepository";
import { useScreenWidth } from "../../../../util/hook/useScreenWidth";
import styles from "./OrdersComponent.module.css";

interface Props {
  orderMessageApiRepository: OrderMessageApiRepository;
  userApiRepository: UserApiRepository;
  orderApiRepository: OrderApiRepository;
  taskApiRepository: TaskApiRepository;
  transportApiRepository: TransportApiRepository;
}

const ORDERS_FILTERING_REGIONS_KEY = "orders_filtering_regions_key";
const ORDERS_FITLERING_STATUSES_KEY = "orders_filtering_statuses_key";
const MINUTES_15_MS = 15 * 60 * 1000;

let lastOrdersRequestTime = Date.now();

export const OrdersComponent = ({
  orderMessageApiRepository,
  userApiRepository,
  orderApiRepository,
  taskApiRepository,
  transportApiRepository,
}: Props): JSX.Element => {
  const screenWidth = useScreenWidth();
  const [isShowMobileChats, setShowMobileChats] = useState(false);

  const [ordersLimit, setOrdersLimit] = useState(100);
  const [isAllOrdersLoaded, setAllOrdersLoaded] = useState(false);

  const [user, setUser] = useState<User | undefined>();

  const [isLoading, setLoading] = useState(false);
  const [orders, setOrders] = useState<Order[]>([]);
  const regions = useRegions();

  const [isShowCreateOrder, setShowCreateOrder] = useState<boolean>(false);
  const [openedOrderId, setOpenedOrderId] = useState<number | undefined>();

  // filters
  const [filteringRegionsIds, setFilteringRegions] = useState<
    string[] | undefined
  >();
  const [filteringStatuses, setFilteringStatuses] = useState<
    string[] | undefined
  >();
  const [filteringUserId, setFilteringUserId] = useState<number | undefined>();

  const loadOrders = async (isSilent = false, isOpenOrderInUrl = false) => {
    if (!filteringRegionsIds || !filteringStatuses) return;

    if (!isSilent) {
      setLoading(true);
    }

    try {
      const currentRequestTime = Date.now();
      lastOrdersRequestTime = currentRequestTime;

      const receivedOrders = await orderApiRepository.getOrders(
        filteringRegionsIds.length > 0
          ? filteringRegionsIds.map((regionId) => +regionId)
          : undefined,
        filteringStatuses.length > 0 ? filteringStatuses : undefined,
        filteringUserId,
        ordersLimit
      );

      if (currentRequestTime === lastOrdersRequestTime) {
        setOrders(receivedOrders);

        if (receivedOrders.length === orders.length) {
          setAllOrdersLoaded(true);
        }
      }

      if (isOpenOrderInUrl) {
        const urlParams = new URLSearchParams(
          window.location.pathname.replace(/\//g, "")
        );
        const orderId = urlParams.get("orderId");
        if (orderId) {
          setOpenedOrderId(+orderId);
          window.history.replaceState(undefined, "", "/");
          window.history.pushState(undefined, "", "/");
        }
      }

      setLoading(false);
    } catch (e) {
      alert("Ошибка при загрузке заказов. Попробуйте перезагрузить страницу");
      console.log((e as Error).message);
    }

    setLoading(false);
  };

  useEffect(() => {
    loadOrders(/* isSilent */ false, /* isOpenOrderInUrl */ true);

    let isRequestInProgress = false;
    const getOrdersInterval = setInterval(async () => {
      if (!isRequestInProgress) {
        isRequestInProgress = true;
        await loadOrders(/* isSilent */ true);
        isRequestInProgress = false;
      }
    }, 1_000);

    return () => {
      clearInterval(getOrdersInterval);
    };
  }, [filteringRegionsIds, filteringStatuses, filteringUserId, ordersLimit]);

  useEffect(() => {
    (async () => {
      try {
        const user = await userApiRepository.getUserById(
          userApiRepository.getAuthorizedData().authorizedUserId
        );
        setUser(user);
      } catch (e) {
        alert("Ошибка при загрузке профиля. Попробуйте перезагрузить страницу");
        console.log((e as Error).message);
      }
    })();
  }, []);

  // restore filters
  useEffect(() => {
    const filteringRegionsIds = localStorage.getItem(
      ORDERS_FILTERING_REGIONS_KEY
    );
    const filteringStatuses = localStorage.getItem(
      ORDERS_FITLERING_STATUSES_KEY
    );

    setFilteringRegions(
      filteringRegionsIds ? JSON.parse(filteringRegionsIds) : []
    );
    setFilteringStatuses(
      filteringStatuses
        ? (JSON.parse(filteringStatuses) as string[]).map((r) =>
            r.toUpperCase()
          )
        : [
            OrderStatus.CREATED,
            OrderStatus.CALCULATING,
            OrderStatus.ACCEPTED,
            OrderStatus.REVIEWING,
          ]
    );

    // filtering user for admin
    const urlParams = new URLSearchParams(window.location.search);
    const filteringUserId = urlParams.get("filteringUserId");

    if (filteringUserId) {
      setFilteringUserId(+filteringUserId);
      window.history.replaceState(undefined, "", "/");
      window.history.pushState(undefined, "", "/");
    }
  }, []);

  useEffect(() => {
    if (isAllOrdersLoaded) {
      setTimeout(() => {
        // we clean this param, because new
        // orders can appear
        setAllOrdersLoaded(false);
      }, 60_000);
    }
  }, [isAllOrdersLoaded]);

  // Here we add invisible cards to make cards
  // look pretty
  let countOfInvisibleCards = 3 - (orders.length % 3);
  if (countOfInvisibleCards === 3) countOfInvisibleCards = 0;
  let invisibleCardsForAlignemnt: JSX.Element[] = [];
  if (countOfInvisibleCards == 1)
    invisibleCardsForAlignemnt = [<div className="w-[320px]" key={`inv_1`} />];
  if (countOfInvisibleCards == 2)
    invisibleCardsForAlignemnt = [
      <div className="w-[320px]" key={`inv_1`} />,
      <div className="w-[320px]" key={`inv_2`} />,
    ];

  const isShowActiveOrders =
    filteringStatuses &&
    filteringStatuses.includes(OrderStatus.CREATED) &&
    filteringStatuses.includes(OrderStatus.CALCULATING) &&
    filteringStatuses.includes(OrderStatus.ACCEPTED) &&
    filteringStatuses.includes(OrderStatus.REVIEWING);

  const now = Date.now();

  return (
    <div>
      <div className="flex">
        <div
          className={`grow mt-6 ${
            user?.role === UserRole.ADMIN ? "mr-10" : ""
          }`}
        >
          <div>
            {user && user?.role !== UserRole.MASTER && (
              <>
                <div className="hidden sm:block">
                  <Button
                    onClick={() => setShowCreateOrder(true)}
                    className="mb-3"
                  >
                    Добавить заказ
                  </Button>
                </div>

                <div className="sm:hidden">
                  <Button
                    onClick={() => setShowCreateOrder(true)}
                    className="mb-3"
                    fullWidth
                  >
                    Добавить заказ
                  </Button>
                </div>
              </>
            )}

            {!user && (
              <div className="flex justify-center items-center h-[36px] w-[147px] mb-3">
                <LoadingComponent size="xs" />
              </div>
            )}

            {user && (
              <div className="mb-3 flex flex-wrap">
                {user?.role !== UserRole.ADMIN && (
                  <div className="max-w-[350px] mr-3">
                    <Select
                      label="Статус заказов"
                      placeholder="Выберите статус"
                      data={[
                        {
                          label: "Активные заявки",
                          value: "active",
                        },
                        {
                          label: "Закрытые заявки",
                          value: "closed",
                        },
                      ]}
                      value={isShowActiveOrders ? "active" : "closed"}
                      onChange={(value) => {
                        let filteringStatuses: OrderStatus[] = [];
                        if (value === "active") {
                          filteringStatuses = [
                            OrderStatus.CREATED,
                            OrderStatus.CALCULATING,
                            OrderStatus.REVIEWING,
                            OrderStatus.ACCEPTED,
                          ];
                        } else {
                          filteringStatuses = [
                            OrderStatus.COMPLETED,
                            OrderStatus.CANCELED,
                          ];
                        }
                        setFilteringStatuses(filteringStatuses);
                        localStorage.setItem(
                          ORDERS_FITLERING_STATUSES_KEY,
                          JSON.stringify(filteringStatuses)
                        );
                        setAllOrdersLoaded(false);
                        setOrdersLimit(100);
                      }}
                    />
                  </div>
                )}

                {user?.role === UserRole.ADMIN && (
                  <>
                    <div className="max-w-[350px] mr-3">
                      <MultiSelect
                        label="Статусы заказов"
                        placeholder="Выберите статусы"
                        clearable
                        data={[
                          {
                            label: OrderStatusNamer.getStatusName(
                              OrderStatus.CREATED
                            ),
                            value: OrderStatus.CREATED,
                          },
                          {
                            label: OrderStatusNamer.getStatusName(
                              OrderStatus.CALCULATING
                            ),
                            value: OrderStatus.CALCULATING,
                          },
                          {
                            label: OrderStatusNamer.getStatusName(
                              OrderStatus.REVIEWING
                            ),
                            value: OrderStatus.REVIEWING,
                          },
                          {
                            label: OrderStatusNamer.getStatusName(
                              OrderStatus.ACCEPTED
                            ),
                            value: OrderStatus.ACCEPTED,
                          },
                          {
                            label: OrderStatusNamer.getStatusName(
                              OrderStatus.COMPLETED
                            ),
                            value: OrderStatus.COMPLETED,
                          },
                          {
                            label: OrderStatusNamer.getStatusName(
                              OrderStatus.CANCELED
                            ),
                            value: OrderStatus.CANCELED,
                          },
                        ]}
                        value={filteringStatuses}
                        onChange={(value) => {
                          setFilteringStatuses(value);
                          localStorage.setItem(
                            ORDERS_FITLERING_STATUSES_KEY,
                            JSON.stringify(value)
                          );
                          setAllOrdersLoaded(false);
                          setOrdersLimit(100);
                        }}
                        searchable
                      />
                    </div>

                    {regions.length > 0 ? (
                      <>
                        <div className="max-w-[350px] mr-3">
                          <MultiSelect
                            clearable
                            label="Регионы заказов"
                            placeholder="Выберите регионы"
                            data={regions.map((region) => {
                              return {
                                label: region.name,
                                value: `${region.id}`,
                              };
                            })}
                            value={filteringRegionsIds}
                            onChange={(value) => {
                              setFilteringRegions(value);
                              localStorage.setItem(
                                ORDERS_FILTERING_REGIONS_KEY,
                                JSON.stringify(value)
                              );
                              setAllOrdersLoaded(false);
                              setOrdersLimit(100);
                            }}
                            searchable
                          />
                        </div>

                        {filteringUserId && (
                          <div className="max-w-[350px] mr-3">
                            <MultiSelect
                              clearable
                              label="Фильтр по пользователям"
                              data={[`ID #${filteringUserId}`]}
                              value={[`ID #${filteringUserId}`]}
                              onChange={() => {
                                setFilteringUserId(undefined);
                                setAllOrdersLoaded(false);
                                setOrdersLimit(100);
                              }}
                            />
                          </div>
                        )}
                      </>
                    ) : (
                      <LoadingComponent size="xs" />
                    )}
                  </>
                )}
              </div>
            )}
          </div>

          {user &&
            user.role !== UserRole.ADMIN &&
            screenWidth &&
            screenWidth < 1280 && (
              <div className="mb-3">
                <Button
                  onClick={() => setShowMobileChats(true)}
                  fullWidth
                  size="xs"
                >
                  Открыть чаты заказов
                </Button>
              </div>
            )}

          {user && (
            <div>
              {orders.length === 0 ? (
                <div className="text-sm text-gray-600">Заказы не найдены</div>
              ) : (
                <div className="hidden xl:block mb-5">
                  <table className="table-sm mb-10">
                    <thead>
                      <tr>
                        <th className="w-[50px] max-w-[50px]">#</th>
                        <th className="w-[115px] max-w-[115px]">Создана</th>
                        <th className="w-[180px] max-w-[180px]">Регион</th>
                        <th className="w-[60px] max-w-[60px]">Тип</th>
                        <th className="w-[120px] max-w-[120px]">Марка</th>
                        <th className="w-[120px] max-w-[120px]">Модель</th>
                        <th className="w-[200px] max-w-[200px]">Статус</th>
                        <th className="w-[100px] max-w-[100px]">Действия</th>
                      </tr>
                    </thead>

                    <tbody>
                      {orders
                        .filter(
                          (order) => !order.declinedMastersIds.includes(user.id)
                        )
                        .map((order) => {
                          const orderTruck = order.transports.find(
                            (auto) => auto.type === TransportType.TRUCK
                          );
                          const orderTrailer = order.transports.find(
                            (auto) => auto.type === TransportType.TRAILER
                          );

                          return (
                            <tr
                              className={`select-none cursor-pointer ${
                                user.role === UserRole.ADMIN &&
                                order.status === OrderStatus.CREATED &&
                                now - MINUTES_15_MS > order.lastStatusUpdateTime
                                  ? `bg-red-100 hover:bg-red-200`
                                  : ""
                              }`}
                              key={`order_row_${order.id}`}
                              onClick={() => setOpenedOrderId(order.id)}
                            >
                              <td className="text-center">{order.id}</td>
                              <td
                                className="text-center min-w-[80px]"
                                style={{ fontSize: "10px", lineHeight: 1.2 }}
                              >
                                <div className="2xl:hidden">
                                  {moment(order.createdAt).format("ll")}
                                </div>
                                <div className="hidden 2xl:block">
                                  {moment(order.createdAt).format("lll")}
                                </div>
                                <div>({moment(order.createdAt).fromNow()})</div>
                              </td>
                              <td>{order.address.region.name}</td>

                              <td>
                                {orderTruck && <div>Грузовик</div>}
                                {orderTrailer && <div>Прицеп</div>}
                              </td>

                              <td>
                                {orderTruck && (
                                  <div>{orderTruck?.brand || "-"}</div>
                                )}
                                {orderTrailer && (
                                  <div>{orderTrailer?.brand || "-"}</div>
                                )}
                              </td>

                              <td>
                                {orderTruck && (
                                  <div>{orderTruck?.model || "-"}</div>
                                )}
                                {orderTrailer && (
                                  <div>{orderTrailer?.model || "-"}</div>
                                )}
                              </td>

                              <td
                                className={`text-center ${OrderStatusNamer.getColorForStatusName(
                                  order.status
                                )}`}
                              >
                                {OrderStatusNamer.getStatusName(order.status)}
                              </td>

                              <td>
                                <Button
                                  variant="outline"
                                  size="xs"
                                  onClick={() => setOpenedOrderId(order.id)}
                                  fullWidth
                                >
                                  См. детали
                                </Button>
                              </td>
                            </tr>
                          );
                        })}
                    </tbody>
                  </table>
                </div>
              )}

              <div className="xl:hidden flex flex-wrap lg:justify-between">
                {orders
                  .filter(
                    (order) => !order.declinedMastersIds.includes(user.id)
                  )
                  .map((order) => (
                    <OrderCardComponent
                      openOrder={() => setOpenedOrderId(order.id)}
                      order={order}
                      key={`order_${order.id}`}
                    />
                  ))}

                {invisibleCardsForAlignemnt.map(
                  (invisibleCard) => invisibleCard
                )}
              </div>
            </div>
          )}

          {(!user || isLoading) && (
            <div className="flex justify-center">
              <LoadingComponent />
            </div>
          )}
          {orders.length > 0 && !isLoading && !isAllOrdersLoaded && (
            <VisiblityDetectorComponent
              onVisible={() => {
                if (!isLoading) {
                  setOrdersLimit(ordersLimit + 100);
                }
              }}
            />
          )}
        </div>

        {user?.role !== UserRole.ADMIN &&
          screenWidth &&
          screenWidth >= 1280 && (
            <div className={`pl-5 max-w-[320px] min-w-[320px] hidden xl:block`}>
              <div className={`border-l ${styles.desktopChatHeight}`}>
                {user ? (
                  <OrdersChatComponent
                    isFullHeight={false}
                    user={user}
                    orderMessageApiRepository={orderMessageApiRepository}
                    ordersIds={orders
                      .filter(
                        (order) =>
                          order.status !== OrderStatus.CREATED &&
                          order.status !== OrderStatus.CANCELED &&
                          order.status !== OrderStatus.COMPLETED
                      )
                      .map((order) => order.id)}
                    isParentLoading={!user || isLoading}
                    setSidebarHidden={() => {}}
                  />
                ) : (
                  <div className="flex justify-center py-3">
                    <LoadingComponent size="xs" />
                  </div>
                )}
              </div>
            </div>
          )}
      </div>

      {openedOrderId && user && (
        <SidebarComponent
          onCloseSidebar={() => {
            setOpenedOrderId(undefined);
          }}
          sidebarName={`Заказ #${openedOrderId}`}
          isFullScreen
          sidebarInner={
            <OrderComponent
              orderMessageApiRepository={orderMessageApiRepository}
              userApiRepository={userApiRepository}
              orderApiRepository={orderApiRepository}
              taskApiRepository={taskApiRepository}
              transportApiRepository={transportApiRepository}
              orderId={openedOrderId}
              user={user}
              isSidebar
              onClose={() => setOpenedOrderId(undefined)}
            />
          }
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
              user={user}
              orderMessageApiRepository={orderMessageApiRepository}
              ordersIds={orders
                .filter(
                  (order) =>
                    order.status !== OrderStatus.CREATED &&
                    order.status !== OrderStatus.CANCELED &&
                    order.status !== OrderStatus.COMPLETED
                )
                .map((order) => order.id)}
              isParentLoading={!user || isLoading}
              setSidebarHidden={() => {}}
            />
          }
        />
      )}

      {isShowCreateOrder && (
        <Modal
          opened={isShowCreateOrder}
          onClose={() => setShowCreateOrder(false)}
          centered
          title="Создание заявки"
        >
          <CreateOrderComponent
            orderApiRepository={orderApiRepository}
            userApiRepository={userApiRepository}
            onOrderCreated={() => {
              loadOrders();
              setShowCreateOrder(false);
            }}
          />
        </Modal>
      )}
    </div>
  );
};

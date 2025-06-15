import { useEffect, useRef, useState } from "react";
import { OrderMessageApiRepository } from "../data/OrderMessageApiRepository";
import { ArrayUtils } from "../../../../util/ArrayUtils";
import { OrderMessage } from "../domain/OrderMessage";
import LoadingComponent from "../../../../util/components/LoadingComponent";
import { OrderDialogComponent } from "./OrderDialogComponent";
import { User } from "../../../user/domain/User";
import { UserRole } from "../../../user/domain/UserRole";
import moment from "moment";
import { Button } from "@mantine/core";
import { MessageFileType } from "../domain/MessageFileType";
import { SoundHelper } from "../../../../util/SoundHelper";
import styles from "./OrdersChatComponent.module.css";

interface Props {
  orderMessageApiRepository: OrderMessageApiRepository;

  isChatClosed?: boolean;
  isSingleChat?: boolean;

  ordersIds: number[];
  user: User;

  isParentLoading: boolean;

  isShowCloseChatsButton?: boolean;
  onCloseChats?(): void;

  isFullHeight?: boolean;

  setSidebarHidden(isHidden: boolean): void;
}

export const OrdersChatComponent = ({
  orderMessageApiRepository,

  isChatClosed,
  isSingleChat,

  ordersIds,
  user,

  isParentLoading,

  isShowCloseChatsButton,
  onCloseChats,

  isFullHeight,

  setSidebarHidden,
}: Props): JSX.Element => {
  const getLastMessagesIntervalRef = useRef<NodeJS.Timeout | undefined>();
  const previousOrdersIdsRef = useRef<number[]>([]);

  const [isLoading, setLoading] = useState(false);
  const [selectedOrderId, setSelectedOrderId] = useState<number | undefined>(
    isSingleChat ? ordersIds[0] : undefined
  );

  const [lastMessageForEachOrder, setLastMessageForEachOrder] = useState<
    OrderMessage[]
  >([]);
  const [ordersIdsWithMessages, setOrdersIdsWithMessages] = useState<number[]>(
    []
  );

  const newPreviousMessagesOrderdsIds = useRef<number[]>([]);
  const [newMessagesOrderdsIds, setNewMessagesOrdersIds] = useState<number[]>(
    []
  );

  const loadLastMessageForEachOrder = async (isSilent = false) => {
    if (!isSilent) {
      setLoading(true);
    }

    try {
      const lastOrdersMessages =
        await orderMessageApiRepository.getLastMessagesPerEachOrder(ordersIds);

      setLastMessageForEachOrder(lastOrdersMessages);
      setOrdersIdsWithMessages(
        lastOrdersMessages.map((message) => message.orderId)
      );

      setNewMessagesOrdersIds(
        lastOrdersMessages
          .filter((orderMessage) => {
            if (user.role === UserRole.DRIVER && !orderMessage.isViewedByDriver)
              return true;
            if (user.role === UserRole.MASTER && !orderMessage.isViewedByMaster)
              return true;
            if (
              user.role === UserRole.CUSTOMER &&
              !orderMessage.isViewedByCustomer
            )
              return true;

            return false;
          })
          // this component is responsible for all orders
          // except opened one
          .filter((message) => message.orderId !== selectedOrderId)
          .map((message) => message.orderId)
      );
    } catch (e) {
      console.log((e as Error).message);
      alert("Не вышло загрузить сообщения. Пожалуйста, перезагрузите страницу");
    }

    setLoading(false);
  };

  useEffect(() => {
    if (
      !ArrayUtils.isEqualNumberSets(previousOrdersIdsRef.current, ordersIds)
    ) {
      if (isSingleChat) setSelectedOrderId(ordersIds[0]);

      let isRequestInProgress = false;

      if (getLastMessagesIntervalRef.current)
        clearInterval(getLastMessagesIntervalRef.current);

      getLastMessagesIntervalRef.current = setInterval(async () => {
        if (!isRequestInProgress) {
          isRequestInProgress = true;
          await loadLastMessageForEachOrder(/* isSilent */ true);
          isRequestInProgress = false;
        }
      }, 3_000);

      loadLastMessageForEachOrder();
    }

    previousOrdersIdsRef.current = ordersIds;
  }, [ordersIds, selectedOrderId]);

  useEffect(() => {
    if (
      newMessagesOrderdsIds.length === 1 &&
      newPreviousMessagesOrderdsIds.current.length === 0
    ) {
      SoundHelper.playMessageSound();
    }

    newPreviousMessagesOrderdsIds.current = newMessagesOrderdsIds;
  }, [newMessagesOrderdsIds]);

  useEffect(() => {
    return () => {
      if (getLastMessagesIntervalRef.current) {
        clearInterval(getLastMessagesIntervalRef.current);
      }
    };
  }, []);

  const getMessageCircle = (): JSX.Element => {
    return (
      <div
        className="w-[15px] h-[15px] bg-blue-700"
        style={{ borderRadius: "360px" }}
      />
    );
  };

  const getMessageItemComponent = (
    orderId: number,
    lastMessage?: OrderMessage
  ): JSX.Element => {
    return (
      <div
        className="border-b h-[70px] hover:bg-gray-50 cursor-pointer p-2 text-xs flex"
        onClick={() => setSelectedOrderId(orderId)}
      >
        <div style={{ width: "80%" }}>
          <div className="font-bold mb-1">Заказ #{orderId}</div>
          {!lastMessage && <div className="text-gray-400">Нет сообщений</div>}

          {lastMessage && (
            <>
              <div
                style={{
                  overflow: "hidden",
                  maxWidth: "100%",
                  whiteSpace: "nowrap",
                }}
              >
                {lastMessage.userRole === UserRole.MASTER ? "СТО" : ""}
                {lastMessage.userRole === UserRole.DRIVER ? "Водитель" : ""}
                {lastMessage.userRole === UserRole.CUSTOMER ? "Перевозчик" : ""}
                : {lastMessage.text ? lastMessage.text : ""}
                {lastMessage.file && (
                  <>
                    {lastMessage.file.type === MessageFileType.IMAGE
                      ? "(изображение)"
                      : "(файл)"}
                  </>
                )}
              </div>

              <div className="opacity-50" style={{ fontSize: "10px" }}>
                {moment(lastMessage.date).format("lll")}
              </div>
            </>
          )}
        </div>

        {lastMessage && (
          <div className="grow flex justify-center items-center">
            {user.role === UserRole.MASTER &&
              !lastMessage.isViewedByMaster &&
              getMessageCircle()}
            {user.role === UserRole.CUSTOMER &&
              !lastMessage.isViewedByCustomer &&
              getMessageCircle()}
            {user.role === UserRole.DRIVER &&
              !lastMessage.isViewedByDriver &&
              getMessageCircle()}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className={isFullHeight ? styles.heightFull : styles.heightSmall}>
      {!selectedOrderId ? (
        <>
          {isShowCloseChatsButton && (
            <div className="mx-1 mt-1 mb-3">
              <Button
                onClick={() => {
                  if (onCloseChats) {
                    onCloseChats();
                  }
                }}
                size="sm"
                fullWidth
              >
                Назад
              </Button>
            </div>
          )}

          {isSingleChat && (
            <div className="flex">
              <div className="mx-auto mt-3 text-sm text-gray-400">
                Чат недоступен
              </div>
            </div>
          )}

          {ordersIds.length === 0 && (
            <div className="flex">
              <div className="mx-auto mt-3 text-sm text-gray-400">
                Нет активных чатов
              </div>
            </div>
          )}

          {!isSingleChat &&
            lastMessageForEachOrder.map((message) => {
              return (
                <div key={`order_${message.orderId}_messages`}>
                  {getMessageItemComponent(message.orderId, message)}
                </div>
              );
            })}

          {(isLoading || isParentLoading) && (
            <div className="flex mt-3 justify-center">
              <LoadingComponent />
            </div>
          )}
        </>
      ) : (
        <OrderDialogComponent
          isChatClosed={isChatClosed}
          isSingleChat={isSingleChat}
          orderMessageApiRepository={orderMessageApiRepository}
          orderId={selectedOrderId}
          user={user}
          onBack={() => {
            setSelectedOrderId(undefined);
            loadLastMessageForEachOrder(/* isSilent */ true);
          }}
          isNewMessagesInAnotherOrder={
            !!lastMessageForEachOrder
              .filter((message) => message.orderId !== selectedOrderId)
              .find((message) => {
                if (user.role === UserRole.MASTER && !message.isViewedByMaster)
                  return true;
                if (user.role === UserRole.DRIVER && !message.isViewedByDriver)
                  return true;
                if (
                  user.role === UserRole.CUSTOMER &&
                  !message.isViewedByCustomer
                )
                  return true;
                return false;
              })
          }
          isShowCloseChatsButton={isShowCloseChatsButton}
          onCloseChats={onCloseChats}
          isFullHeight={isFullHeight}
          setSidebarHidden={setSidebarHidden}
        />
      )}
    </div>
  );
};

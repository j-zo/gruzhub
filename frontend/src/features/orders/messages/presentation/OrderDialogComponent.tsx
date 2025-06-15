/* eslint-disable @next/next/no-img-element */
import { useEffect, useRef, useState } from "react";
import { OrderMessageApiRepository } from "../data/OrderMessageApiRepository";
import { OrderMessage } from "../domain/OrderMessage";
import { Button } from "@mantine/core";
import styles from "./OrderDialogComponent.module.css";
import Image from "next/image";
import LoadingComponent from "../../../../util/components/LoadingComponent";
import { User } from "../../../user/domain/User";
import { UserRole } from "../../../user/domain/UserRole";
import moment from "moment";
import { MessageFileType } from "../domain/MessageFileType";
import { APPLICATION_SERVER } from "../../../../constants";
import { ImageDialogComponent } from "./ImageDialogComponent";
import StringUtils from "../../../../util/StringUtils";
import { FilesUtils } from "../../../../util/FilesUtils";
import { MessageFile } from "../domain/MessageFile";

interface Props {
  orderMessageApiRepository: OrderMessageApiRepository;

  isSingleChat?: boolean;
  isChatClosed?: boolean;

  orderId: number;
  user: User;
  isNewMessagesInAnotherOrder: boolean;

  onBack(): void;

  isShowCloseChatsButton?: boolean;
  onCloseChats?(): void;

  isFullHeight?: boolean;

  setSidebarHidden(isHidden: boolean): void;
}

let permanentSavingFilesCodes: string[] = [];

export const OrderDialogComponent = ({
  orderMessageApiRepository,

  isSingleChat,
  isChatClosed,

  orderId,
  user,
  isNewMessagesInAnotherOrder,

  onBack,

  isShowCloseChatsButton,
  onCloseChats,

  isFullHeight,

  setSidebarHidden,
}: Props): JSX.Element => {
  const messagesBottomRef = useRef<HTMLDivElement>(null);

  const [isLoading, setLoading] = useState(false);
  const [isUsersLoaded, setUsersLoaded] = useState(false);
  const [isFilesUploading, setFilesUploading] = useState(false);

  const [messages, setMessages] = useState<OrderMessage[]>([]);
  const messagesCountRef = useRef(0);

  const [savingFilesCodes, setSavingFilesCodes] = useState<string[]>([]);

  const [users, setUsers] = useState<User[]>([]);

  const [text, setText] = useState("");

  const [openedImageUrl, setOpenedImageUrl] = useState<string | undefined>();

  const markMessagesAsRead = async () => {
    if (user.role === UserRole.ADMIN) return;

    try {
      await orderMessageApiRepository.setMessagesViewedByCurrentUserRole(
        orderId
      );
      await loadMessages(/* isSilent */ true);
    } catch (e) {
      console.log((e as Error).message);
      alert(
        "Не вышло отметить сообщения прочитанными. Пожалуйста, перезагрузите страницу"
      );
    }
  };

  const scrollToMessagesBottom = () => {
    setTimeout(() => {
      if (messagesBottomRef)
        messagesBottomRef.current?.scrollIntoView({ behavior: "smooth" });
    }, 100);
  };

  const loadOrderUsers = async () => {
    try {
      setUsers(await orderMessageApiRepository.getOrderMessagesUsers(orderId));
      setUsersLoaded(true);
    } catch (e) {
      console.log((e as Error).message);
      alert("Не вышло загрузить пользователей. Перезагрузите страницу");
    }
  };

  const loadMessages = async (isSilent = false) => {
    if (!isSilent) {
      setLoading(true);
    }

    try {
      const previousMessagesLength = messagesCountRef.current;
      const newMessages = await orderMessageApiRepository.getOrderMessages(
        orderId
      );
      setMessages(newMessages);

      if (newMessages.length !== previousMessagesLength) {
        scrollToMessagesBottom();
        markMessagesAsRead();
      }

      messagesCountRef.current = newMessages.length;
    } catch (e) {
      console.log((e as Error).message);
      alert("Не вышло загрузить сообщения. Перезагрузите страницу");
    }

    setLoading(false);
  };

  const sendMessage = async () => {
    if (!text) return;

    try {
      setText("");

      messages.push({
        id: undefined as unknown as number,
        orderId: orderId,
        userId: user.id,
        userRole: user.role,
        text: text,
        date: Date.now(),
        isViewedByMaster: user.role === UserRole.MASTER,
        isViewedByDriver: user.role === UserRole.DRIVER,
        isViewedByCustomer: user.role === UserRole.CUSTOMER,
      });

      await orderMessageApiRepository.sendMessage(orderId, text);
    } catch (e) {
      console.log((e as Error).message);
      alert("Не вышло отправить сообщение. Перезагрузите страницу");
    }
  };

  const validateFiles = (files: File[]): boolean => {
    for (const file of files) {
      if (
        !/\.(png)$/i.test(file.name) &&
        !/\.(jpg)$/i.test(file.name) &&
        !/\.(webp)$/i.test(file.name) &&
        !/\.(svg)$/i.test(file.name) &&
        !/\.(jpeg)$/i.test(file.name) &&
        !/\.(mpeg)$/i.test(file.name) &&
        !/\.(webm)$/i.test(file.name) &&
        !/\.(ogg)$/i.test(file.name) &&
        !/\.(avi)$/i.test(file.name) &&
        !/\.(flv)$/i.test(file.name) &&
        !/\.(wmv)$/i.test(file.name) &&
        !/\.(pdf)$/i.test(file.name) &&
        !/\.(doc)$/i.test(file.name) &&
        !/\.(docx)$/i.test(file.name) &&
        !/\.(xls)$/i.test(file.name) &&
        !/\.(xlsx)$/i.test(file.name) &&
        !/\.(csv)$/i.test(file.name) &&
        !/\.(xml)$/i.test(file.name) &&
        !/\.(mp4)$/i.test(file.name)
      ) {
        alert("Поддерживаются только PNG, JPG, WEBP, SVG и JPEG");
        return false;
      }

      const MBYTES_100_IN_BYTES = 104_857_600;
      if (file.size > MBYTES_100_IN_BYTES) {
        alert("Нельзя загрузить слишком большой файл");
        return false;
      }
    }

    return true;
  };

  const sendFilesMessages = async (files: File[]) => {
    if (!validateFiles(files)) return;

    setFilesUploading(true);

    try {
      for (const file of files) {
        const filenameSplittedByDots = file.name.split(".");
        await orderMessageApiRepository.sendFileMessage(
          orderId,
          filenameSplittedByDots
            .slice(0, filenameSplittedByDots.length - 1)
            .join("."),
          filenameSplittedByDots[filenameSplittedByDots.length - 1],
          file
        );
      }
    } catch (e) {
      console.log((e as Error).message);
      alert("Не вышло отправить файл");
    }

    setFilesUploading(false);
  };

  const saveFile = async (file: MessageFile) => {
    if (permanentSavingFilesCodes.includes(file.code || "")) return;

    permanentSavingFilesCodes.push(file.code);
    setSavingFilesCodes(JSON.parse(JSON.stringify(permanentSavingFilesCodes)));

    try {
      const fileUrl = `${APPLICATION_SERVER}/api/files/${file.code}.${file.extension}`;
      const response = await fetch(fileUrl);
      const contentBytes = await response.blob();
      FilesUtils.download(
        contentBytes,
        `${file.filename}.${file.extension}`,
        file.contentType
      );
      // window.open(url, "_blank").focus();
    } catch (e) {}

    permanentSavingFilesCodes.splice(
      permanentSavingFilesCodes.indexOf(file.code),
      1
    );
    setSavingFilesCodes(JSON.parse(JSON.stringify(permanentSavingFilesCodes)));
  };

  useEffect(() => {
    messagesCountRef.current = 0;
    setMessages([]);
    setUsersLoaded(false);
    setLoading(true);
    permanentSavingFilesCodes = [];
    setSavingFilesCodes([]);

    loadMessages(/* isSilent */ false).then(() => scrollToMessagesBottom());
    loadOrderUsers();

    let isRequestInProgress = false;
    const getMessagesInterval = setInterval(async () => {
      if (!isRequestInProgress) {
        isRequestInProgress = true;
        await loadMessages(/* isSilent */ true);
        isRequestInProgress = false;
      }
    }, 1_000);

    return () => {
      clearInterval(getMessagesInterval);
    };
  }, [orderId]);

  const getMesageComponent = (orderMessage: OrderMessage): JSX.Element => {
    let userRoleInText = "";
    if (orderMessage.userRole === UserRole.MASTER) userRoleInText = "СТО";
    if (orderMessage.userRole === UserRole.CUSTOMER)
      userRoleInText = "перевозчик";
    if (orderMessage.userRole === UserRole.DRIVER) userRoleInText = "водитель";

    return (
      <div className="flex" key={`order_${orderId}_message_${orderMessage.id}`}>
        <div
          className={`rounded-lg py-2 px-2 mb-1 ${
            orderMessage.userRole === user.role ||
            (orderMessage.userRole === UserRole.CUSTOMER &&
              user.role === UserRole.ADMIN)
              ? "ml-auto bg-blue-700 text-white"
              : "mr-auto bg-blue-100"
          }`}
          style={{ maxWidth: "80%" }}
        >
          <div className="mb-1" style={{ fontSize: "10px" }}>
            {isUsersLoaded
              ? `${
                  users.find((user) => user.id === orderMessage.userId)?.name ||
                  ""
                } (${userRoleInText})`
              : userRoleInText}
          </div>

          {orderMessage.text && (
            <div
              className="mb-1"
              style={{
                fontSize: "13px",
                whiteSpace: "pre-line",
              }}
            >
              {orderMessage.text}
            </div>
          )}

          {orderMessage.file &&
            orderMessage.file.type === MessageFileType.IMAGE && (
              <img
                onClick={() => {
                  setOpenedImageUrl(
                    `${APPLICATION_SERVER}/api/files/${orderMessage.file?.code}.${orderMessage.file?.extension}`
                  );
                  setSidebarHidden(true);
                }}
                src={`${APPLICATION_SERVER}/api/files/${orderMessage.file?.code}.${orderMessage.file.extension}`}
                alt={orderMessage.file.filename}
                loading="lazy"
                style={{
                  width: "200px",
                  height: "200px",
                  objectFit: "contain",
                  cursor: "pointer",
                }}
              />
            )}

          {orderMessage.file &&
            orderMessage.file.type === MessageFileType.FILE && (
              <div
                className={`flex mb-2 cursor-pointer ${
                  savingFilesCodes.includes(orderMessage.file.code)
                    ? "opacity-60"
                    : ""
                }`}
                onClick={() => {
                  if (orderMessage.file) saveFile(orderMessage.file);
                }}
              >
                <div>
                  <img
                    className="mr-2"
                    src="/document.svg"
                    loading="lazy"
                    style={{
                      width: "30px",
                      height: "30px",
                      objectFit: "contain",
                      cursor: "pointer",
                    }}
                    alt="Document"
                  />
                </div>

                <div>
                  <div style={{ fontSize: "13px" }}>
                    {StringUtils.cutBySymbolsLength(
                      orderMessage.file.filename,
                      20
                    )}
                    .{orderMessage.file.extension}
                  </div>
                  <div></div>
                </div>
              </div>
            )}

          <div className="flex">
            <div className="opacity-50" style={{ fontSize: "10px" }}>
              {moment(orderMessage.date).format("lll")}
            </div>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div
      className={`flex flex-col ${
        isFullHeight ? styles.heightFull : styles.heightSmall
      }`}
    >
      {!isSingleChat && (
        <div className="p-1">
          <Button onClick={() => onBack()} size="sm" fullWidth>
            Назад
          </Button>

          {isNewMessagesInAnotherOrder && (
            <div
              className="w-[10px] h-[10px] bg-white absolute"
              style={{
                borderRadius: "360px",
                marginTop: "-30px",
                marginLeft: "5px",
              }}
            />
          )}
        </div>
      )}

      {isSingleChat && isShowCloseChatsButton && (
        <div className="p-1">
          <Button
            onClick={() => {
              if (onCloseChats) onCloseChats();
            }}
            size="sm"
            fullWidth
          >
            Назад
          </Button>
        </div>
      )}
      <div className="text-sm font-bold my-2 flex justify-center">
        Чат заказа #{orderId}
      </div>

      {isLoading ? (
        <div
          className={`flex justify-center pt-3 ${styles.messagesFullHeight}`}
        >
          <LoadingComponent size="xs" />
        </div>
      ) : (
        <>
          {messages.length > 0 && (
            <div
              className={`mt-auto p-1 overflow-y-auto ${styles.messages} ${styles.messagesFullHeight}`}
            >
              {messages.map((message) => getMesageComponent(message))}

              <div
                ref={messagesBottomRef}
                id="messages-bottom"
                style={{ width: "100%", height: "1px" }}
              />
            </div>
          )}

          {messages.length === 0 && (
            <div
              className={`text-center text-gray-400 ${styles.messagesFullHeight}`}
            >
              Нет сообщений
            </div>
          )}
        </>
      )}

      {isChatClosed || user.role === UserRole.ADMIN ? (
        <div
          className={`h-[100px] flex ${
            messages.length > 0 ? "mt-2" : "mt-auto"
          } border-t`}
        >
          <div className="text-center text-gray-400 w-full mt-4">
            Отправка сообщений недоступна
          </div>
        </div>
      ) : (
        <div
          className={`h-[100px] flex ${
            messages.length > 0 ? "mt-1" : "mt-auto"
          } border-t`}
        >
          <textarea
            className={`h-[93px] ${styles.textarea} grow p-2`}
            value={text}
            onChange={(e) => setText(e.target.value.slice(0, 10_000))}
            placeholder="Введите сообщение"
            style={{ resize: "none" }}
            onKeyDown={(e) => {
              if (e.keyCode == 13 && e.shiftKey) {
                sendMessage();
              }
            }}
          />

          <div>
            <div
              className="h-1/2 flex justify-center hover:opacity-80"
              onClick={() => sendMessage()}
            >
              <Image
                src="/icons/right-arrow.svg"
                alt="Send"
                width={40}
                height={40}
              />
            </div>

            {isFilesUploading ? (
              <div className="h-1/2 flex justify-center cursor-pointer hover:opacity-80 pt-3">
                <LoadingComponent size="xs" />
              </div>
            ) : (
              <>
                <input
                  type="file"
                  style={{ display: "none", height: "50px" }}
                  onChange={(e) => {
                    if (
                      e.currentTarget.files &&
                      e.currentTarget.files.length > 0
                    ) {
                      sendFilesMessages(Array.from(e.currentTarget.files));
                      // @ts-ignore
                      e.target.value = null;
                    }
                  }}
                  id="upload-button"
                  accept=".jpg,.jpeg,.png,.webp,.svg,.mpeg,.mp4,.webm,.ogg,.avi,.flv,.wmv,.pdf,.doc,.docx,.xls,.xlsx,.csv,.xml"
                  multiple
                />

                <label
                  className="h-1/2 flex justify-center cursor-pointer hover:opacity-80"
                  htmlFor="upload-button"
                >
                  <Image
                    src="/icons/attachment.svg"
                    alt="Send"
                    width={27}
                    height={27}
                  />
                </label>
              </>
            )}
          </div>
        </div>
      )}

      {openedImageUrl && (
        <ImageDialogComponent
          imageUrl={openedImageUrl}
          onClose={() => {
            setOpenedImageUrl("");
            setSidebarHidden(false);
          }}
        />
      )}
    </div>
  );
};

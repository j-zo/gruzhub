import { Modal } from "@mantine/core";
import moment from "moment";
import "moment/locale/ru";
import { User } from "../../../../user/domain/User";
import { OrderApiRepository } from "../../data/OrderApiRepository";
import { useEffect, useState } from "react";
import { UserInfoChange } from "../../domain/UserInfoChange";
import LoadingComponent from "../../../../../util/components/LoadingComponent";
import StringUtils from "../../../../../util/StringUtils";
import { UserRole } from "../../../../user/domain/UserRole";

interface Props {
  orderApiRepository: OrderApiRepository;

  orderId: number;
  user: User;
  close(): void;

  isLoadStatusChanges: boolean;
}

export const UserDetailsComponent = ({
  orderApiRepository,
  orderId,
  user,
  close,
  isLoadStatusChanges,
}: Props): JSX.Element => {
  const [isLoading, setLoading] = useState(false);
  const [userInfoChanges, setUserInfoChanges] = useState<UserInfoChange[]>([]);

  useEffect(() => {
    if (isLoadStatusChanges) {
      (async () => {
        setLoading(true);

        try {
          setUserInfoChanges(
            await orderApiRepository.getUserInfoChanges(user.id, orderId)
          );
        } catch (e) {
          console.log((e as Error).message);
          alert("Не вышло загрузить данные водителя. Перезагрузите страницу");
        }

        setLoading(false);
      })();
    }
  }, []);

  let userTitle = "";
  if (user.role === UserRole.DRIVER) userTitle = "водителя";
  if (user.role === UserRole.MASTER) userTitle = "автосервиса";
  if (user.role === UserRole.CUSTOMER) userTitle = "заказчика";
  if (user.role === UserRole.ADMIN) userTitle = "администратора";

  return (
    <Modal
      title={`Детали ${userTitle} (#${user.id})`}
      opened
      onClose={() => close()}
    >
      <div className="flex text-sm max-w-full overflow-x-hidden">
        <div className="w-[120px] min-w-[120px]">Регистрация:</div>{" "}
        {moment(user.registrationDate).format("lll")} (
        {moment(user.registrationDate).fromNow()})
      </div>

      <div className="flex text-sm max-w-full overflow-x-hidden">
        <div className="w-[120px] min-w-[120px]">
          {user.role === UserRole.DRIVER ? "Имя" : "Название"}:
        </div>{" "}
        {user.name || "-"}
      </div>

      <div className="flex text-sm max-w-full overflow-x-hidden">
        <div className="w-[120px] min-w-[120px]">Телефон:</div>{" "}
        {user.phone ? StringUtils.formatAsPhone(user.phone) : undefined || "-"}
      </div>

      <div className="flex text-sm max-w-full overflow-x-hidden">
        <div className="w-[120px] min-w-[120px]">Почта:</div>{" "}
        {user.email || "-"}
      </div>

      <div className="mt-5">
        {isLoading ? (
          <LoadingComponent size="xs" />
        ) : (
          <>
            {userInfoChanges.length > 0 && (
              <>
                <div className="font-bold mb-3">Изменения в данных</div>

                {userInfoChanges.map((info) => {
                  return (
                    <div
                      className="border mb-1 text-xs p-3 rounded"
                      key={`user_info_${info.id}`}
                    >
                      {(info.previousName || info.newName) && (
                        <div className="flex max-w-full overflow-x-hidden">
                          <div className="w-[70px]">Имя:</div>{" "}
                          <div className="w-[130px]">
                            {info.previousName || "-"}
                          </div>
                          <div className="w-[50px] flex justify-center">
                            {"-->"}
                          </div>
                          <div className="w-[130px]">{info.newName || "-"}</div>
                        </div>
                      )}

                      {(info.previousPhone || info.newPhone) && (
                        <div className="flex max-w-full overflow-x-hidden">
                          <div className="w-[70px]">Телефон:</div>{" "}
                          <div className="w-[130px]">
                            {info.previousPhone
                              ? StringUtils.formatAsPhone(info.previousPhone)
                              : undefined || "-"}
                          </div>
                          <div className="w-[50px] flex justify-center">
                            {"-->"}
                          </div>
                          <div className="w-[130px]">
                            {info.newPhone
                              ? StringUtils.formatAsPhone(info.newPhone)
                              : undefined || "-"}
                          </div>
                        </div>
                      )}

                      {(info.previousEmail || info.newEmail) && (
                        <div className="flex max-w-full overflow-x-hidden">
                          <div className="w-[70px]">Почта:</div>{" "}
                          <div className="w-[130px] break-all">
                            {info.previousEmail || "-"}
                          </div>
                          <div className="w-[50px] flex justify-center">
                            {"-->"}
                          </div>
                          <div className="w-[130px] break-all">
                            {info.newEmail || "-"}
                          </div>
                        </div>
                      )}

                      {(info.previousInn || info.newInn) && (
                        <div className="flex max-w-full overflow-x-hidden">
                          <div className="w-[70px]">ИНН:</div>{" "}
                          <div className="w-[130px] break-all">
                            {info.previousInn || "-"}
                          </div>
                          <div className="w-[50px] flex justify-center">
                            {"-->"}
                          </div>
                          <div className="w-[130px] break-all">
                            {info.newInn || "-"}
                          </div>
                        </div>
                      )}

                      <div className="mt-3">
                        Дата: {moment(info.date).format("lll")} (
                        {moment(info.date).fromNow()})
                      </div>
                    </div>
                  );
                })}
              </>
            )}
          </>
        )}
      </div>
    </Modal>
  );
};

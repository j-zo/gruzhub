import { useEffect, useState } from "react";
import { AutoApiRepository } from "../auto/AutoApiRepository";
import { Auto } from "../domain/Auto";
import LoadingComponent from "../../../../util/components/LoadingComponent";
import { Autocomplete, Button, TextInput } from "@mantine/core";
import { AutoType } from "../domain/AutoType";
import { TRAILER_TYPE } from "../../../../util/trailer_constants";
import { TRUCK_MODELS } from "../../../../util/truck_constants";
import { User } from "../../../user/domain/User";
import { UserRole } from "../../../user/domain/UserRole";
import { Order } from "../../orders/domain/Order";
import { OrderStatus } from "../../orders/domain/OrderStatus";

interface Props {
  autoApiRepository: AutoApiRepository;

  autoId: number;
  order: Order;
  user: User;
}

export const OrderAutoComponent = ({
  autoApiRepository,

  order,
  autoId,
  user,
}: Props): JSX.Element => {
  const [isSaving, setSaving] = useState(false);
  const [isUnsaved, setUnsaved] = useState(false);
  const [isEditing, setEditing] = useState(false);

  const [auto, setAuto] = useState<Auto | undefined>();
  const [autoBeforeEdit, setAutoBeforeEdit] = useState<Auto | undefined>();

  const loadAuto = async () => {
    setSaving(true);

    try {
      setAuto(await autoApiRepository.getOrderAuto(order.id, autoId));
    } catch (e) {
      console.log((e as Error).message);
      alert(
        "Не вышло загрузить данные машины. Пожалуйста, перезагрузите страницу"
      );
    }

    setSaving(false);
  };

  const startEdit = () => {
    setEditing(true);
    setAutoBeforeEdit(JSON.parse(JSON.stringify(auto)));
  };

  const cancelEdit = () => {
    setEditing(false);
    setAuto(JSON.parse(JSON.stringify(autoBeforeEdit)));
    setUnsaved(false);
  };

  const save = async () => {
    if (!auto) throw new Error("auto should be defined here");

    try {
      await autoApiRepository.updateOrderAuto(order.id, autoId, auto);
      setEditing(false);
      setUnsaved(false);
    } catch (e) {
      console.log((e as Error).message);
      alert(
        "Не вышло сохранить данные машины. Пожалуйста, перезагрузите страницу"
      );
    }
  };

  useEffect(() => {
    loadAuto();

    let isRequestInProgress = false;
    let updateInterval: NodeJS.Timeout | undefined;

    if (!isEditing) {
      updateInterval = setInterval(async () => {
        if (!isRequestInProgress) {
          isRequestInProgress = true;
          await loadAuto();
          isRequestInProgress = false;
        }
      }, 10_000);
    }

    return () => {
      if (updateInterval) clearInterval(updateInterval);
    };
  }, [isEditing]);

  return (
    <>
      {auto ? (
        <>
          <div className="font-bold mb-3">
            {auto.type === AutoType.TRUCK ? "Грузовик" : "Прицеп"}
          </div>

          {isEditing ? (
            <>
              {auto.type === AutoType.TRUCK && (
                <div className="flex text-sm items-center mb-1">
                  <div className="w-[100px] min-w-[100px]">Марка:</div>
                  <TextInput
                    value={auto.brand}
                    onChange={(e) => {
                      auto.brand = e.currentTarget.value;
                      setAuto(JSON.parse(JSON.stringify(auto)));
                    }}
                    size="xs"
                  />
                </div>
              )}

              <div className="flex text-sm items-center mb-1">
                <div className="w-[100px] min-w-[100px]">
                  {auto.type === AutoType.TRUCK ? "Модель" : "Тип"}:
                </div>

                {auto.type === AutoType.TRUCK ? (
                  <Autocomplete
                    placeholder="Actros"
                    data={TRUCK_MODELS[auto.brand || ""]}
                    value={auto.model}
                    onChange={(value) => {
                      auto.model = value;
                      setAuto(JSON.parse(JSON.stringify(auto)));
                      setUnsaved(true);
                    }}
                    size="xs"
                  />
                ) : (
                  <Autocomplete
                    placeholder="Тентованный"
                    data={TRAILER_TYPE}
                    value={auto.model}
                    onChange={(value) => {
                      auto.model = value;
                      setAuto(JSON.parse(JSON.stringify(auto)));
                      setUnsaved(true);
                    }}
                    size="xs"
                  />
                )}
              </div>

              <div className="flex text-sm items-center mb-1">
                <div className="w-[100px] min-w-[100px]">VIN:</div>
                <TextInput
                  value={auto.vin}
                  onChange={(e) => {
                    auto.vin = e.currentTarget.value;
                    setAuto(JSON.parse(JSON.stringify(auto)));
                    setUnsaved(true);
                  }}
                  size="xs"
                />
              </div>

              <div className="flex text-sm items-center mb-1">
                <div className="w-[100px] min-w-[100px]">Гос. номер:</div>
                <TextInput
                  value={auto.number}
                  onChange={(e) => {
                    auto.number = e.currentTarget.value;
                    setAuto(JSON.parse(JSON.stringify(auto)));
                    setUnsaved(true);
                  }}
                  size="xs"
                />
              </div>
            </>
          ) : (
            <>
              {auto.type === AutoType.TRUCK && (
                <div className="flex text-sm">
                  <div className="w-[100px] min-w-[100px]">Марка:</div>
                  {auto.brand || "-"}
                </div>
              )}

              <div className="flex text-sm">
                <div className="w-[100px] min-w-[100px]">
                  {auto.type === AutoType.TRUCK ? "Модель" : "Тип"}:
                </div>
                {auto.model || "-"}
              </div>

              <div className="flex text-sm">
                <div className="w-[100px] min-w-[100px]">VIN:</div>
                {auto.vin || "-"}
              </div>

              <div className="flex text-sm">
                <div className="w-[100px] min-w-[100px]">Гос. номер:</div>
                {auto.number || "-"}
              </div>
            </>
          )}

          {(user.role !== UserRole.MASTER ||
            order.status !== OrderStatus.CREATED) && (
            <Button
              onClick={() => {
                if (isEditing) {
                  cancelEdit();
                } else {
                  startEdit();
                }
              }}
              className="mt-2"
              size="xs"
              variant="outline"
              disabled={isSaving}
            >
              {isEditing ? "Отменить" : "Редактировать"}
            </Button>
          )}

          {isEditing && isUnsaved && (
            <Button
              onClick={() => {
                save();
              }}
              className="mt-2 ml-1"
              size="xs"
              disabled={isSaving}
            >
              Сохранить
            </Button>
          )}
        </>
      ) : (
        <LoadingComponent size="xs" />
      )}
    </>
  );
};

import { useEffect, useState } from "react";
import { TransportApiRepository } from "@/features/common/transport/data/TransportApiRepository";
import { Transport } from "../domain/Transport";
import LoadingComponent from "../../../../util/components/LoadingComponent";
import { Autocomplete, Button, TextInput } from "@mantine/core";
import { TransportType } from "../domain/TransportType";
import { TRAILER_TYPE } from "../../../../util/trailer_constants";
import { TRUCK_MODELS } from "../../../../util/truck_constants";
import { User } from "../../../user/domain/User";
import { UserRole } from "../../../user/domain/UserRole";
import { Order } from "../../../orders/orders/domain/Order";
import { OrderStatus } from "../../../orders/orders/domain/OrderStatus";

interface Props {
  transportApiRepository: TransportApiRepository;

  transportId: number;
  order: Order;
  user: User;
}

export const OrderTransportComponent = ({
  transportApiRepository,
  order,
  transportId,
  user,
}: Props): JSX.Element => {
  const [isSaving, setSaving] = useState(false);
  const [isUnsaved, setUnsaved] = useState(false);
  const [isEditing, setEditing] = useState(false);

  const [transport, setTransport] = useState<Transport | undefined>();
  const [transportBeforeEdit, settransportBeforeEdit] = useState<Transport | undefined>();

  const loadTransport = async () => {
    setSaving(true);

    try {
      setTransport(await transportApiRepository.getOrderTransport(order.id, transportId));
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
    settransportBeforeEdit(JSON.parse(JSON.stringify(transport)));
  };

  const cancelEdit = () => {
    setEditing(false);
    setTransport(JSON.parse(JSON.stringify(transportBeforeEdit)));
    setUnsaved(false);
  };

  const save = async () => {
    if (!transport) throw new Error("Transport should be defined here");

    try {
      await transportApiRepository.updateOrderTransport(order.id, transportId, transport);
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
    loadTransport();

    let isRequestInProgress = false;
    let updateInterval: NodeJS.Timeout | undefined;

    if (!isEditing) {
      updateInterval = setInterval(async () => {
        if (!isRequestInProgress) {
          isRequestInProgress = true;
          await loadTransport();
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
      {transport ? (
        <>
          <div className="font-bold mb-3">
            {transport.type === TransportType.TRUCK ? "Грузовик" : "Прицеп"}
          </div>

          {isEditing ? (
            <>
              {transport.type === TransportType.TRUCK && (
                <div className="flex text-sm items-center mb-1">
                  <div className="w-[100px] min-w-[100px]">Марка:</div>
                  <TextInput
                    value={transport.brand}
                    onChange={(e) => {
                      transport.brand = e.currentTarget.value;
                      setTransport(JSON.parse(JSON.stringify(transport)));
                    }}
                    size="xs"
                  />
                </div>
              )}

              <div className="flex text-sm items-center mb-1">
                <div className="w-[100px] min-w-[100px]">
                  {transport.type === TransportType.TRUCK ? "Модель" : "Тип"}:
                </div>

                {transport.type === TransportType.TRUCK ? (
                  <Autocomplete
                    placeholder="Actros"
                    data={TRUCK_MODELS[transport.brand || ""]}
                    value={transport.model}
                    onChange={(value) => {
                      transport.model = value;
                      setTransport(JSON.parse(JSON.stringify(transport)));
                      setUnsaved(true);
                    }}
                    size="xs"
                  />
                ) : (
                  <Autocomplete
                    placeholder="Тентованный"
                    data={TRAILER_TYPE}
                    value={transport.model}
                    onChange={(value) => {
                      transport.model = value;
                      setTransport(JSON.parse(JSON.stringify(transport)));
                      setUnsaved(true);
                    }}
                    size="xs"
                  />
                )}
              </div>

              <div className="flex text-sm items-center mb-1">
                <div className="w-[100px] min-w-[100px]">VIN:</div>
                <TextInput
                  value={transport.vin}
                  onChange={(e) => {
                    transport.vin = e.currentTarget.value;
                    setTransport(JSON.parse(JSON.stringify(transport)));
                    setUnsaved(true);
                  }}
                  size="xs"
                />
              </div>

              <div className="flex text-sm items-center mb-1">
                <div className="w-[100px] min-w-[100px]">Гос. номер:</div>
                <TextInput
                  value={transport.number}
                  onChange={(e) => {
                    transport.number = e.currentTarget.value;
                    setTransport(JSON.parse(JSON.stringify(transport)));
                    setUnsaved(true);
                  }}
                  size="xs"
                />
              </div>
            </>
          ) : (
            <>
              {transport.type === TransportType.TRUCK && (
                <div className="flex text-sm">
                  <div className="w-[100px] min-w-[100px]">Марка:</div>
                  {transport.brand || "-"}
                </div>
              )}

              <div className="flex text-sm">
                <div className="w-[100px] min-w-[100px]">
                  {transport.type === TransportType.TRUCK ? "Модель" : "Тип"}:
                </div>
                {transport.model || "-"}
              </div>

              <div className="flex text-sm">
                <div className="w-[100px] min-w-[100px]">VIN:</div>
                {transport.vin || "-"}
              </div>

              <div className="flex text-sm">
                <div className="w-[100px] min-w-[100px]">Гос. номер:</div>
                {transport.number || "-"}
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

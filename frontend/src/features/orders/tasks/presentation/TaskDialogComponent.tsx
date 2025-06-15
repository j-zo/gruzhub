import { useState } from "react";
import { TaskApiRepository } from "../data/TaskApiRepository";
import { Order } from "../../orders/domain/Order";
import { Button, Modal, TextInput, Textarea } from "@mantine/core";
import { AutoType } from "../../auto/domain/AutoType";
import { Task } from "../domain/Task";

interface Props {
  taskApiRepository: TaskApiRepository;

  task?: Task;
  order: Order;
  onTaskUpdated(): void;
  closeDialog(): void;
}

export const TaskDialogComponent = ({
  taskApiRepository,

  task,
  order,
  onTaskUpdated,
  closeDialog,
}: Props): JSX.Element => {
  const [isSaving, setSaving] = useState(false);

  const [autoId, setAutoId] = useState(task?.autoId || order.autos[0].id);
  const [name, setName] = useState(task?.name || "");
  const [description, setDescription] = useState(task?.description || "");
  const [price, setPrice] = useState(task?.price ? Number(task.price) : 0 || 0);

  const [isNameError, setNameError] = useState(false);

  const updateTask = async () => {
    if (!name) {
      setNameError(true);
      return;
    }

    setSaving(true);

    try {
      const taskToUpdate: Task = {
        id: task?.id || (undefined as unknown as number),
        name: name,
        description: description,
        price: `${price}`,
        orderId: order.id,
        autoId: autoId,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      };

      if (task?.id) {
        await taskApiRepository.updateTask(taskToUpdate);
      } else {
        await taskApiRepository.createTask(taskToUpdate);
      }

      onTaskUpdated();
    } catch (e) {
      alert(
        `Не вышло ${
          task?.id ? "обновить" : "добавить"
        } работу. Перезагрузите страницу и попробуйте снова`
      );
    }

    setSaving(false);
  };

  return (
    <Modal
      title="Добавление ремонтной работы"
      opened
      onClose={() => closeDialog()}
      centered
    >
      <div>
        <div className="text-sm font-medium">
          Выберите транспортное средство
        </div>

        <select
          className="form-control mt-1"
          value={autoId}
          onChange={(e) => setAutoId(Number(e.target.value))}
        >
          {order.autos.map((auto) => (
            <option key={`auto_option_${auto.id}`} value={auto.id}>{`${
              auto.type === AutoType.TRUCK ? "Грузовик" : "Прицеп"
            }: ${auto.model ? `${auto.brand} ` : ``}${auto.model}`}</option>
          ))}
        </select>
      </div>

      <TextInput
        className="mt-1"
        label="Название работы"
        placeholder="Замена колеса"
        required
        value={name}
        onChange={(e) => {
          setName(e.currentTarget.value);
          setNameError(false);
        }}
        error={isNameError}
      />

      <Textarea
        className="mt-1"
        label="Описание работы"
        placeholder="Уточните детали, если требуется"
        value={description}
        onChange={(e) => {
          setDescription(e.currentTarget.value);
        }}
      />

      <TextInput
        className="mt-1"
        label="Стоимость работы"
        type="number"
        value={price}
        onChange={(e) => {
          setPrice(Number(e.currentTarget.value));
        }}
      />

      <Button
        className="mt-3"
        fullWidth
        disabled={isSaving}
        onClick={() => updateTask()}
      >
        {task?.id ? "Обновить" : "Добавить"} работу
      </Button>
    </Modal>
  );
};

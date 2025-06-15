import { Button, Checkbox, Modal, Textarea } from "@mantine/core";
import { useState } from "react";

interface Props {
  onConfirm(comment?: string): void;
  onDecline(): void;

  reasons?: string[];

  description: string;
  actionButtonColor: "blue" | "red";

  actionText: string;
}

export const ConfirmationComponent = ({
  onConfirm,
  onDecline,

  reasons,

  description,
  actionButtonColor,
  actionText,
}: Props): JSX.Element => {
  const [selectedReasons, setSelectedReasons] = useState<string[]>([]);
  const [comment, setComment] = useState("");

  const confirm = () => {
    if (reasons && (selectedReasons.length === 0 && !comment)) {
      alert(
        "Пожалуйста, выберите причину или укажите причину в комментариях"
      );
      return;
    }

    let reason = "";

    selectedReasons.forEach((selectedReason) => {
      reason += `- ${selectedReason}\n`;
    });

    if (selectedReasons.length > 0 && comment) {
      reason += "\n";
    }
    if (comment) reason += comment;

    onConfirm(reason);
  };

  return (
    <Modal title="Подтверждение" opened onClose={() => onDecline()}>
      <div>{description}</div>

      {reasons && (
        <>
          <div className="mt-5 font-medium text-sm">Укажите причину</div>

          <div>
            {reasons.map((reason) => {
              return (
                <div key={`reason_${reason}`}>
                  <Checkbox
                    className="mt-2"
                    checked={selectedReasons.includes(reason)}
                    onChange={() => {
                      if (selectedReasons.includes(reason)) {
                        selectedReasons.splice(
                          selectedReasons.indexOf(reason),
                          1
                        );
                      } else {
                        selectedReasons.push(reason);
                      }
                      setSelectedReasons(
                        JSON.parse(JSON.stringify(selectedReasons))
                      );
                    }}
                    label={reason}
                    size="xs"
                  />
                </div>
              );
            })}
          </div>

          <div className="mt-3">
            <Textarea
              rows={3}
              value={comment}
              onChange={(e) => setComment(e.currentTarget.value)}
              label="Другая причина или комментарий"
              placeholder="Укажите причину (или оставьте отзыв)"
            />
          </div>
        </>
      )}

      <div className="mt-5 flex">
        <Button
          className="ml-auto"
          onClick={() => onDecline()}
          color={actionButtonColor === "red" ? "blue" : "red"}
        >
          Отменить
        </Button>

        <Button
          className="ml-1"
          onClick={() => confirm()}
          color={actionButtonColor === "red" ? "red" : "blue"}
        >
          {actionText}
        </Button>
      </div>
    </Modal>
  );
};

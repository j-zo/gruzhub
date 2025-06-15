import { TextInput } from "@mantine/core";
import { ReactNode } from "react";
import StringUtils from "../StringUtils";

export type PhoneInputProps = {
  phone?: string;
  label: string;
  error?: ReactNode;
  onChange: (phone: string) => void;
  required?: boolean;
  disabled?: boolean;
};

export function PhoneInput(props: PhoneInputProps) {
  return (
    <TextInput
      label="Номер телефона"
      placeholder="+7 (XXX) XXX-XX-XX"
      value={StringUtils.formatAsPhone(props.phone || "")}
      error={props.error}
      required={props.required}
      onChange={(e) => {
        const notFormattedPhone = e.currentTarget.value;
        if (
          notFormattedPhone.length ===
          StringUtils.formatAsPhone(props.phone || "").length - 1
        ) {
          // Means deleted special char (like "-"), which
          // has not been passed here due to formatting clean
          props.onChange(
            (props.phone || "").slice(0, (props.phone || "").length - 1)
          );
          return;
        }

        const lastChar = notFormattedPhone[notFormattedPhone.length - 1];
        const isNotDigitRegex = /[^\d]/;
        if (isNotDigitRegex.test(lastChar)) return;

        const phone = StringUtils.cleanPhoneFormatting(notFormattedPhone);
        props.onChange(phone);
      }}
      disabled={props.disabled}
      autoComplete="off"
    />
  );
}

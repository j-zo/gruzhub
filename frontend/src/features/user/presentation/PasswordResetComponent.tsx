import React, { useState } from "react";
import FormValidator from "../../../util/FormValidator";
import UserApiRepository from "../data/UserApiRepository";
import { Button, TextInput } from "@mantine/core";
import { AuthRoleSelector } from "./AuthRoleSelector";
import { UserRole } from "../domain/UserRole";
import Link from "next/link";
import { Pages } from "../../../constants";

interface Props {
  userApiRepository: UserApiRepository;
}

export const PasswordResetComponent = ({
  userApiRepository,
}: Props): JSX.Element => {
  const [isLoading, setLoading] = useState(false);

  const [userRole, setUserRole] = useState<UserRole>(UserRole.CUSTOMER);
  const [email, setEmail] = useState("");
  const [emailError, setEmailError] = useState(false);

  const [responseSuccessText, setResponseSuccessText] = useState("");
  const [responseErrorText, setResponseErrorText] = useState("");

  const validateFieldsForReset = (): boolean => {
    let isValid = true;

    if (!email || !FormValidator.isValidEmail(email)) {
      setEmailError(true);
      isValid = false;
    } else {
      setEmailError(false);
    }

    return isValid;
  };

  const onReset = async () => {
    setLoading(true);
    setResponseErrorText("");

    if (validateFieldsForReset()) {
      try {
        await userApiRepository.sendResetEmail(email, userRole);
        setResponseSuccessText(
          "Ссылка для восстановления была отправлена вам на почту (проверьте спам)"
        );
      } catch (e) {
        setResponseErrorText((e as Error).message);
      }
    }

    setLoading(false);
  };

  return (
    <>
      <div className="flex justify-center mb-4">
        <div className="text-xl font-bold">Восстановление пароля</div>
      </div>

      <AuthRoleSelector userRole={userRole} setUserRole={setUserRole} />

      <TextInput
        label="Почта"
        placeholder="Введите почту для восстановления"
        value={email}
        error={emailError}
        onChange={(e) => {
          setEmailError(false);
          setEmail(e.currentTarget.value);
        }}
      />

      {responseErrorText && (
        <div className="my-3 text-sm flex justify-center text-red-600 text-center mx-2">
          {responseErrorText}
        </div>
      )}

      {responseSuccessText ? (
        <div className="mt-3 text-sm flex justify-center text-green-600 text-center mx-2">
          {responseSuccessText}
        </div>
      ) : undefined}

      {!responseSuccessText && (
        <Button
          onClick={() => {
            onReset();
          }}
          className="mt-2"
          disabled={isLoading}
          fullWidth
        >
          Отправить код восстановления
        </Button>
      )}

      <div className="flex justify-center text-sm mt-3">
        <Link
          className="text-blue-700 cursor-pointer"
          href={Pages.HOME_PAGE}
        >
          Регистрация
        </Link>
      </div>
    </>
  );
};

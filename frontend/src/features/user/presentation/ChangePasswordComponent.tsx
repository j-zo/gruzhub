import { useEffect, useState } from "react";
import UserApiRepository from "../data/UserApiRepository";
import { Button, TextInput } from "@mantine/core";
import { UserRole } from "../domain/UserRole";

interface Props {
  userApiRepository: UserApiRepository;
}

export const ChangePasswordComponent = ({
  userApiRepository,
}: Props): JSX.Element => {
  const [userRole, setUserRole] = useState<UserRole | undefined>();
  const [isLoading, setLoading] = useState(false);
  const [resetCode, setResetCode] = useState("");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordError, setPasswordError] = useState(false);
  const [confirmPassword, setConfirmPassword] = useState("");
  const [confirmPasswordError, setConfirmPasswordError] = useState(false);

  const [resetError, setResetError] = useState("");

  const onChange = async () => {
    setLoading(true);
    setPasswordError(false);
    setConfirmPasswordError(false);
    setResetError("");

    if (validateFieldsForChangePassword()) {
      if (!userRole) throw new Error("NPE");

      try {
        await userApiRepository.resetPassword(
          email,
          resetCode,
          password,
          userRole
        );
        await userApiRepository.signIn(
          email,
          /* phone */ undefined,
          password,
          userRole
        );
        alert("Пароль был изменен!");
        window.location.href = "/";
      } catch (e) {
        setResetError((e as Error).message);
      }
    }

    setLoading(false);
  };

  const validateFieldsForChangePassword = (): boolean => {
    let isValid = true;

    if (!password) {
      setPasswordError(true);
      isValid = false;
    } else {
      setPasswordError(false);
    }

    if (!confirmPassword) {
      setConfirmPasswordError(true);
      isValid = false;
    } else {
      setConfirmPasswordError(false);
    }

    if (password !== confirmPassword) {
      setConfirmPasswordError(true);
      isValid = false;
    } else {
      setConfirmPasswordError(false);
    }

    return isValid;
  };

  useEffect(() => {
    setResetCode(
      (new URLSearchParams(window.location.search).get("code") || "").replace(
        /\//g,
        ""
      )
    );
    setEmail(
      (new URLSearchParams(window.location.search).get("email") || "").replace(
        /\//g,
        ""
      )
    );

    const role = new URLSearchParams(window.location.search).get("role") || "";
    if (role === UserRole.CUSTOMER) setUserRole(UserRole.CUSTOMER);
    if (role === UserRole.MASTER) setUserRole(UserRole.MASTER);
  }, []);

  return (
    <>
      <div className="flex justify-center mb-4">
        <div className="text-xl font-bold">Смена пароля</div>
      </div>

      <TextInput label="Почта" value={email} disabled />

      <TextInput
        label="Пароль"
        placeholder="Новый пароль"
        value={password}
        error={passwordError}
        onChange={(e) => {
          setPasswordError(false);
          setPassword(e.currentTarget.value);
        }}
        type="password"
      />

      <TextInput
        label="Подтвердите пароль"
        placeholder="Повторите пароль"
        value={confirmPassword}
        onChange={(e) => {
          setConfirmPassword(e.currentTarget.value);
          setConfirmPasswordError(false);
        }}
        error={confirmPasswordError}
        type="password"
      />

      {resetError && (
        <div className="my-3 text-sm flex justify-center text-red-600 text-center mx-2">
          {resetError}
        </div>
      )}

      <Button
        onClick={() => {
          onChange();
        }}
        className="mt-2"
        disabled={isLoading}
        fullWidth
      >
        Сменить пароль
      </Button>
    </>
  );
};

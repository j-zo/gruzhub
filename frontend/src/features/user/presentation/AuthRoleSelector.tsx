import { UserRole } from "../domain/UserRole";
import styles from "./AuthRoleSelector.module.css";

interface Props {
  userRole: UserRole;
  setUserRole(userRole: UserRole): void;
}

export const AuthRoleSelector = ({
  userRole,
  setUserRole,
}: Props): JSX.Element => {
  return (
    <>
      <div className="text-sm flex justify-center mb-1">
        Выберите вид деятельности компании
      </div>

      <div className="flex justify-center mb-7">
        <div
          className={`${styles.tab} ${styles.tabLeft} ${
            userRole === UserRole.MASTER && styles.tabSelected
          } text-sm`}
          onClick={() => setUserRole(UserRole.MASTER)}
        >
          Автосервис
        </div>

        <div
          className={`${styles.tab} ${styles.tabRight} ${
            userRole === UserRole.CUSTOMER && styles.tabSelected
          } text-sm`}
          onClick={() => setUserRole(UserRole.CUSTOMER)}
        >
          Перевозчик
        </div>
      </div>
    </>
  );
};

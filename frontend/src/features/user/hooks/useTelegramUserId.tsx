import { useEffect, useState } from "react";

export function useTelegramUserId(): number | undefined {
  const [userTgId, setUserTgId] = useState<number | undefined>(undefined);

  useEffect(() => {
    // @ts-ignore
    const userTgId = window.Telegram?.WebApp?.initDataUnsafe.user?.id;
    // @ts-ignore
    console.log(`Telegram\n${JSON.stringify(window.Telegram, undefined, 4)}`);
    if (userTgId) setUserTgId(userTgId);

    console.log(`userTgId: ${userTgId}`);
  }, []);

  return userTgId;
}

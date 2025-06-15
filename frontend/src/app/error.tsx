"use client";

import { useEffect } from "react";

export default function Error({
  error,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.log(`error: ${(error as Error).message}\n\n${error.stack}`);

    if (window.location.href.includes("localhost")) return;
    const errorMessage = `${error.message}\n\n${error.stack}`.slice(0, 4000);

    fetch(
      `https://api.click-chat.ru/api/dialog/send-direct-text-message?websiteUuid=4c895200-0726-4cac-bf67-04b1b93cd39a&chatUuid=dca10766-23d2-43c8-82c2-b77a2bfa101a&text=${encodeURIComponent(
        errorMessage
      )}&isMarkdown=false`
    ).catch((e) => {
      console.log(`log error: ${(e as Error).message}`);
    });
  }, [error]);

  return (
    <div>
      <h2 className="mt-20 max-w-lg mx-auto text-center">
        Произошла ошибка. Мы получили уведомление о ней и постараемся решить
        проблему в течение нескольких часов
      </h2>
    </div>
  );
}

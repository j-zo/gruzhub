import { MessageFileType } from "./MessageFileType";

export interface MessageFile {
  code: string;
  filename: string;
  extension: string;
  contentType: string;
  type: MessageFileType;
  userId: number;
  createdAt: number;
}

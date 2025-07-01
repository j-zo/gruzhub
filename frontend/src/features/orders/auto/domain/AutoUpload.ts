import {AutoUploadStatus} from "@/features/orders/auto/domain/AutoUploadStatus";

export interface Auto {
    autoId: number;
    status?: AutoUploadStatus;
    message?: string;
}

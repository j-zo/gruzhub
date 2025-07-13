import {TransportUploadStatus} from "@/features/common/transport/domain/TransportUploadStatus";

export interface Auto {
    autoId: number;
    status?: TransportUploadStatus;
    message?: string;
}

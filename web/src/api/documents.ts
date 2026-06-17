import { http, unwrapResponse } from './http'
import type {
  DocumentDetail,
  DocumentListItem,
  DocumentParseStatus,
  DocumentUploadResult,
} from './types'

export interface UploadDocumentPayload {
  spaceId: number
  file: File
  title?: string
  industry?: string
  serviceLine?: string
  confidentialLevel?: string
}

export async function listDocuments(spaceId: number): Promise<DocumentListItem[]> {
  return unwrapResponse(await http.get(`/api/v1/kb-spaces/${spaceId}/documents`))
}

export async function getDocument(documentId: number): Promise<DocumentDetail> {
  return unwrapResponse(await http.get(`/api/v1/documents/${documentId}`))
}

export async function deleteDocument(documentId: number): Promise<{ documentId: number; status: string }> {
  return unwrapResponse(await http.delete(`/api/v1/documents/${documentId}`))
}

export async function getDocumentParseStatus(documentId: number): Promise<DocumentParseStatus> {
  return unwrapResponse(await http.get(`/api/v1/documents/${documentId}/parse-status`))
}

export async function uploadDocument(payload: UploadDocumentPayload): Promise<DocumentUploadResult> {
  const formData = new FormData()
  formData.append('file', payload.file)
  if (payload.title) formData.append('title', payload.title)
  if (payload.industry) formData.append('industry', payload.industry)
  if (payload.serviceLine) formData.append('serviceLine', payload.serviceLine)
  if (payload.confidentialLevel) formData.append('confidentialLevel', payload.confidentialLevel)

  return unwrapResponse(await http.post(`/api/v1/kb-spaces/${payload.spaceId}/documents`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  }))
}

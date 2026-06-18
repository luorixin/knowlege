import { http, unwrapResponse } from './http'
import type { EntityId, TaskCenterItem } from './types'

export interface ListTaskCenterParams {
  spaceId: EntityId
  status?: string
  taskCategory?: string
  limit?: number
}

export async function listTaskCenter(params: ListTaskCenterParams): Promise<TaskCenterItem[]> {
  return unwrapResponse(await http.get('/api/v1/tasks/center', { params }))
}

export async function runTask(taskKey: string): Promise<TaskCenterItem> {
  return unwrapResponse(await http.post(`/api/v1/tasks/center/${taskKey}/run`))
}

export async function retryTask(taskKey: string): Promise<TaskCenterItem> {
  return unwrapResponse(await http.post(`/api/v1/tasks/center/${taskKey}/retry`))
}

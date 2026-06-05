import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ChatMessage {
  rol: 'usuario' | 'asistente';
  texto: string;
  timestamp: string;
}

export interface ChatSession {
  id: string;
  title: string;
  messages: ChatMessage[];
  createdAt: string;
  updatedAt: string;
}

const STORAGE_KEY = 'tallersoft_chat_history';

@Injectable({ providedIn: 'root' })
export class ChatHistoryService {
  private sessions$ = new BehaviorSubject<ChatSession[]>([]);

  constructor() {
    this.load();
  }

  get sessions() {
    return this.sessions$.asObservable();
  }

  get sessionsList(): ChatSession[] {
    return this.sessions$.value;
  }

  private load(): void {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      this.sessions$.next(raw ? JSON.parse(raw) : []);
    } catch {
      this.sessions$.next([]);
    }
  }

  private persist(): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.sessions$.value));
  }

  getSession(id: string): ChatSession | undefined {
    return this.sessions$.value.find(s => s.id === id);
  }

  saveSession(session: ChatSession): void {
    const list = this.sessions$.value.filter(s => s.id !== session.id);
    list.unshift(session);
    this.sessions$.next(list);
    this.persist();
  }

  deleteSession(id: string): void {
    const list = this.sessions$.value.filter(s => s.id !== id);
    this.sessions$.next(list);
    this.persist();
  }

  generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).slice(2);
  }
}

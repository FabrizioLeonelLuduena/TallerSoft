import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ProfileState {
  nombre: string;
  avatarGradient: number;
  avatarImage: string | null;
}

const STORAGE_KEY = 'ts_profile';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private _state: ProfileState = this.load();
  private _subject = new BehaviorSubject<ProfileState>({ ...this._state });
  readonly profile$ = this._subject.asObservable();

  private load(): ProfileState {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : { nombre: '', avatarGradient: 0, avatarImage: null };
    } catch {
      return { nombre: '', avatarGradient: 0, avatarImage: null };
    }
  }

  update(patch: Partial<ProfileState>): void {
    this._state = { ...this._state, ...patch };
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(this._state)); } catch (e) {
      console.warn('[ProfileService] localStorage write failed:', e);
    }
    this._subject.next({ ...this._state });
  }

  get snapshot(): ProfileState { return { ...this._state }; }

  /** Compress and resize an image File to a compact base64 JPEG (≤200×200px). */
  compressImage(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (ev) => {
        const img = new Image();
        img.onload = () => {
          const MAX = 200;
          const ratio = Math.min(MAX / img.width, MAX / img.height, 1);
          const canvas = document.createElement('canvas');
          canvas.width  = Math.round(img.width  * ratio);
          canvas.height = Math.round(img.height * ratio);
          canvas.getContext('2d')!.drawImage(img, 0, 0, canvas.width, canvas.height);
          resolve(canvas.toDataURL('image/jpeg', 0.85));
        };
        img.onerror = reject;
        img.src = ev.target!.result as string;
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }
}

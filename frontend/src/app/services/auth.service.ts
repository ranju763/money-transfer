import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { environment } from 'environments/environment';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private readonly API_ENDPOINT = environment.baseUrl + '/auth/login';

    public accountId: string | null = null;
    public authToken: string | null = null;
    public loggedIn: boolean = false;

    constructor(private http: HttpClient) { }

    login(username: string, password: string): Observable<HttpResponse<any>> {
        this.accountId = username;
        this.authToken = 'Basic ' + btoa(username + ':' + password);

        return this.http.get(this.API_ENDPOINT, {
            observe: 'response',
        }).pipe(
            tap(response => {
                if (response.ok) {
                    this.loggedIn = true;
                } else {
                    this.logout();
                }
            }),
            catchError((err) => {
                this.logout();
                return throwError(() => err);
            })
        );
    }

    logout(): void {
        this.loggedIn = false;
        this.accountId = null;
        this.authToken = null;
    }

    /**
     * Verify a password against the one used at login (decoded from the Basic token).
     * Used to gate sensitive views like the balance.
     */
    checkPassword(password: string): boolean {
        if (!this.authToken) return false;
        try {
            const decoded = atob(this.authToken.replace('Basic ', ''));
            const original = decoded.substring(decoded.indexOf(':') + 1);
            return original.length > 0 && original === password;
        } catch {
            return false;
        }
    }
}

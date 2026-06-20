import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AccountMaskDirective } from 'app/directives/account-mask.directive';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [ReactiveFormsModule, CommonModule, AccountMaskDirective],
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css']
})
export class LoginComponent {
    loginForm!: FormGroup;
    submitted = signal(false);
    isLoading = signal(false);
    errorMessage = signal("");

    constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) { }

    ngOnInit() {
        this.loginForm = this.fb.group({
            username: ['', [Validators.required, Validators.minLength(14), Validators.maxLength(14)]],
            password: ['', [Validators.required, Validators.minLength(8)]]
        });
    }

    onSubmit() {
        // set to true
        this.submitted.set(true)

        if (this.loginForm.valid) {
            this.isLoading.set(true)
            this.errorMessage.set("")
            const { username, password } = this.loginForm.value;
            this.authService.login(username, password).subscribe({
                next: (response) => {
                    if (response.ok) {
                        this.router.navigate(['/dashboard']);
                    } else {
                        this.errorMessage.set("Invalid credentials")
                    }
                    this.isLoading.set(false)
                },
                error: (err) => {
                    this.errorMessage.set("Invalid credentials")
                    this.isLoading.set(false)
                }
            });
        }
    }
}

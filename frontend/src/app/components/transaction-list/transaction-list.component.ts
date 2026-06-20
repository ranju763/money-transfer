import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Transaction } from '../../models/transaction.model';
import { AuthService } from '../../services/auth.service';
import { AccountService } from 'app/services/account.service';
import { environment } from 'environments/environment';

@Component({
    selector: 'app-transaction-list',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './transaction-list.component.html',
    styleUrls: ['./transaction-list.component.css']
})
export class TransactionListComponent implements OnInit {
    readonly RECORD_PER_PAGE=environment.pageLimit;

    protected Math = Math;
    errorMessage = signal<string>('');

    // Pagination
    currentPage = signal<number>(1);

    // Filter Menu UI
    isFilterMenuOpen = signal<boolean>(false);
    activeFilterCategory = signal<'Status' | 'Type' | 'Date'>('Date');

    // Filter Options
    searchTerm = signal<string>('');
    selectedStatusFilters = signal<'SUCCESS' | 'FAILED' | null>(null);
    selectedTypeFilters = signal<'SEND' | 'RECEIVE' | null>(null);
    dateSortOrder = signal<'asc' | 'desc'>('desc');

    // Computed signal for filtered and sorted transactions with running balance
    allTransactions = signal<Transaction[]>([]);
    filteredTransactions = computed(() => {
        let data = [...this.allTransactions()];

        // Apply Search Filter (HolderName)
        if (this.searchTerm().length != 0) {
            const term = this.searchTerm().toLowerCase();
            data = data.filter(t => 
                t.holderName.toLowerCase().includes(term)
                || t.amount.toString().includes(term)
            );
        }

        // Apply Status Filter
        if (this.selectedStatusFilters() !== null) {
            data = data.filter(t => this.selectedStatusFilters() === t.status.toUpperCase());
        }

        // Apply Type Filter
        if (this.selectedTypeFilters() !== null) {
            data = data.filter(t =>  this.selectedTypeFilters() === t.type.toUpperCase());
        }

        // Apply Date Sort
        data.sort((a, b) => {
            const dateA = new Date(a.createdOn).getTime();
            const dateB = new Date(b.createdOn).getTime();
            return this.dateSortOrder() === 'desc' ? dateB - dateA : dateA - dateB;
        });

        return data;
    });
    appliedFilters = computed(() => {
        return (
            (this.selectedStatusFilters() !== null ? 1 : 0)
            + (this.selectedTypeFilters() !== null ? 1 : 0)
            + (this.dateSortOrder() !== 'desc' ? 1 : 0)
        )
    })
    totalPages = computed(() => Math.ceil(this.filteredTransactions().length / this.RECORD_PER_PAGE) || 1);
    transactions = computed(() => {
        const start = (this.currentPage() - 1) * this.RECORD_PER_PAGE
        return this.filteredTransactions().slice(start, start + this.RECORD_PER_PAGE);
    });

    // ---- Hero summary (across all transactions, not just the current page/filter) ----
    totalCount = computed(() => this.allTransactions().length);
    totalSent = computed(() =>
        this.allTransactions()
            .filter(t => t.type === 'SEND' && t.status === 'SUCCESS')
            .reduce((sum, t) => sum + Number(t.amount), 0)
    );
    totalReceived = computed(() =>
        this.allTransactions()
            .filter(t => t.type === 'RECEIVE' && t.status === 'SUCCESS')
            .reduce((sum, t) => sum + Number(t.amount), 0)
    );

    constructor(
        private authService: AuthService,
        private accountService: AccountService
    ) { }

    ngOnInit(): void {
        if (this.authService.loggedIn) {
            this.fetchTransactions();
        } else {
            this.errorMessage.set("Session expired. Please re-login.")
        }
    }

    fetchTransactions(): void {
        if (this.authService.accountId) {
            this.accountService.fetchTransactions(this.authService.accountId).subscribe({
                next: (data) => {
                    this.allTransactions.set(data);
                },
                error: (_) => {
                    this.errorMessage.set('Failed to load transactions. Please re-login')
                }
            });
        }
    }

    toggleFilterMenu(): void {
        this.isFilterMenuOpen.update(v => !v);
    }

    changeFilterCategory(category: 'Status' | 'Type' | 'Date'): void {
        this.activeFilterCategory.set(category);
    }

    clearFilters(): void {
        this.selectedStatusFilters.set(null);
        this.selectedTypeFilters.set(null);
        this.dateSortOrder.set('desc');
        this.currentPage.set(1);
        
        // Typical user behavior on clearFilter would be to close
        // the filter menu, therefore we call toggleFilterMenu
        this.toggleFilterMenu()
    }

    setStatusFilter(value: 'SUCCESS' | 'FAILED'): void {
        this.selectedStatusFilters.set(value);
        this.currentPage.set(1);
    }

    setTypeFilter(value: 'SEND' | 'RECEIVE'): void {
        this.selectedTypeFilters.set(value);
        this.currentPage.set(1);
    }

    setDateSort(order: 'asc' | 'desc'): void {
        this.dateSortOrder.set(order);
    }

    setSearchTerm(event: Event): void {
        const value = (event.target as HTMLInputElement).value;
        this.searchTerm.set(value);
        this.currentPage.set(1);
    }

    // Pagination functionality
    nextPage(): void {
        if (this.currentPage() < this.totalPages()) {
            this.currentPage.update(p => p + 1);
        }
    }

    prevPage(): void {
        if (this.currentPage() > 1) {
            this.currentPage.update(p => p - 1);
        }
    }
}

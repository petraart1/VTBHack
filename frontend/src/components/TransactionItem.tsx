import { ArrowDownLeft, ArrowUpRight, CreditCard, ShoppingBag } from 'lucide-react';
import { Transaction } from '../types';

interface TransactionItemProps {
  transaction: Transaction;
  bankName?: string;
}

export const TransactionItem = ({ transaction, bankName }: TransactionItemProps) => {
  const getIcon = () => {
    switch (transaction.transaction_type) {
      case 'cash-in':
        return <ArrowDownLeft className="w-5 h-5 text-green-600" />;
      case 'cash-out':
      case 'transfer':
        return <ArrowUpRight className="w-5 h-5 text-red-600" />;
      case 'purchase':
        return <ShoppingBag className="w-5 h-5 text-blue-600" />;
      default:
        return <CreditCard className="w-5 h-5 text-gray-600" />;
    }
  };

  const getStatusColor = () => {
    switch (transaction.status) {
      case 'confirmed':
        return 'bg-green-100 text-green-800';
      case 'pending':
        return 'bg-yellow-100 text-yellow-800';
      case 'cancelled':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const formatAmount = (amount: number) => {
    const sign = transaction.transaction_type === 'cash-in' ? '+' : '-';
    const formatted = Math.abs(amount).toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return `${sign}${formatted} ₽`;
  };

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('ru-RU', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getStatusText = () => {
    switch (transaction.status) {
      case 'confirmed':
        return 'Подтверждено';
      case 'pending':
        return 'В ожидании';
      case 'cancelled':
        return 'Отменено';
      default:
        return transaction.status;
    }
  };

  return (
    <div className="flex items-center justify-between p-4 bg-white dark:bg-gray-800 rounded-xl hover:shadow-md hover:shadow-blue-500/10 dark:hover:shadow-purple-500/10 transition-shadow border border-transparent hover:border-blue-200/50 dark:hover:border-purple-500/30">
      <div className="flex items-center gap-4">
        <div className="bg-gray-100 dark:bg-gray-700 p-3 rounded-full">
          {getIcon()}
        </div>
        <div>
          <p className="font-semibold text-gray-800 dark:text-gray-200">{transaction.description}</p>
          <div className="flex items-center gap-2 mt-1">
            <p className="text-sm text-gray-500 dark:text-gray-400">{formatDate(transaction.transaction_date)}</p>
            {bankName && (
              <span className="text-xs px-2 py-1 rounded-full bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-500 dark:to-purple-500 text-white font-semibold">
                {bankName}
              </span>
            )}
            <span className={`text-xs px-2 py-1 rounded-full ${getStatusColor()}`}>
              {getStatusText()}
            </span>
          </div>
        </div>
      </div>
      <div className="text-right">
        <p className={`font-bold text-lg ${transaction.transaction_type === 'cash-in' ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`}>
          {formatAmount(transaction.amount)}
        </p>
      </div>
    </div>
  );
};

package com.xowl.spending.usecases.repository;

import com.xowl.spending.entities.Spending;
import java.util.List;

public interface SpendingRepository {
    int getSpendingListSize();

    void saveSpendingList(List<Spending> spendingList);
}

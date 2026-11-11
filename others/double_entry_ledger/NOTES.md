The three tools every test uses
when(...).thenReturn(...) tells a fake what to answer. when(userRepository.existsByEmail("a@b.com")).thenReturn(true) means “if the service asks whether this email exists, say yes.”
assertThrows and assertEquals check the result. The first confirms that the code throws the expected exception. The second confirms that a returned value is what you expect.
verify(...) checks what the service did. verify(tenantRepository).save(any()) means “the service must have called save once,” and never() means “it must not have.”

Each test follows the same pattern: set up the fakes, call the method, check the outcome.

Walking through one test
java
@Test
void registerTenant_noUser() {
    when(userRepository.existsByEmail("a@b.com")).thenReturn(false);   // 1

    assertThrows(IllegalArgumentException.class,                        // 2
            () -> service.registerTenant(new CreateTenantRequest("a@b.com", "t-1", "T")));
    verify(tenantRepository, never()).save(any());                      // 3
}
The fake user repository says no user has that email.
The real service runs registerTenant and should throw IllegalArgumentException because there’s no such user.
The service must not have saved a tenant, so no half-finished data gets created.
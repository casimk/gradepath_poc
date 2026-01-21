import { Injectable, BadRequestException, UnauthorizedException, Logger } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';

import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { AuthResponseDto } from './dto/auth-response.dto';

interface UserDto {
  id: string;
  username?: string;
  email: string;
  displayName?: string;
}

interface SpringBootAuthResponse {
  user: UserDto;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);
  private readonly coreServiceUrl: string;

  constructor(
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
    private readonly httpService: HttpService,
  ) {
    // Spring Boot service URL from environment or default
    this.coreServiceUrl = this.configService.get<string>('GRADEPATH_CORE_URL') ||
      `http://${this.configService.get('GRADEPATH_CORE_HOST') || 'gradepath-core'}:${this.configService.get('GRADEPATH_CORE_PORT') || '8081'}`;
    this.logger.log(`Spring Boot service URL: ${this.coreServiceUrl}`);
  }

  async register(registerDto: RegisterDto): Promise<AuthResponseDto> {
    this.logger.log(`Registering new user: ${registerDto.username}`);

    try {
      const response = await firstValueFrom(
        this.httpService.post<SpringBootAuthResponse>(
          `${this.coreServiceUrl}/api/auth/register`,
          {
            username: registerDto.username,
            email: registerDto.email,
            password: registerDto.password,
            displayName: registerDto.displayName,
          },
        ),
      );

      return this.mapToAuthResponseDto(response.data);
    } catch (error) {
      this.logger.error(`Registration failed: ${error.message}`);
      if (error.response?.status === 400) {
        throw new BadRequestException(error.response.data?.message || 'Registration failed');
      }
      throw new BadRequestException('Failed to register user');
    }
  }

  async login(loginDto: LoginDto): Promise<AuthResponseDto> {
    this.logger.log(`Login attempt: ${loginDto.username || loginDto.email}`);

    try {
      const response = await firstValueFrom(
        this.httpService.post<SpringBootAuthResponse>(
          `${this.coreServiceUrl}/api/auth/login`,
          {
            username: loginDto.username,
            email: loginDto.email,
            password: loginDto.password,
          },
        ),
      );

      return this.mapToAuthResponseDto(response.data);
    } catch (error) {
      this.logger.error(`Login failed: ${error.message}`);
      if (error.response?.status === 401) {
        throw new UnauthorizedException('Invalid credentials');
      }
      throw new UnauthorizedException('Login failed');
    }
  }

  async refreshToken(refreshToken: string): Promise<AuthResponseDto> {
    this.logger.log('Refreshing token');

    try {
      // Extract userId from JWT token if available
      // For now, we'll need to pass userId from the frontend
      // This is a limitation we'll address by storing userId in the JWT
      const decoded = this.jwtService.decode(refreshToken) as any;
      const userId = decoded?.sub || decoded?.userId;

      if (!userId) {
        throw new UnauthorizedException('Invalid refresh token - no userId found');
      }

      const response = await firstValueFrom(
        this.httpService.post<SpringBootAuthResponse>(
          `${this.coreServiceUrl}/api/auth/refresh`,
          {
            userId,
            refreshToken,
          },
        ),
      );

      return this.mapToAuthResponseDto(response.data);
    } catch (error) {
      this.logger.error(`Token refresh failed: ${error.message}`);
      if (error.response?.status === 401) {
        throw new UnauthorizedException('Invalid refresh token');
      }
      throw new UnauthorizedException('Token refresh failed');
    }
  }

  async logout(userId: string, refreshToken: string): Promise<void> {
    this.logger.log(`Logging out user: ${userId}`);

    try {
      await firstValueFrom(
        this.httpService.post(
          `${this.coreServiceUrl}/api/auth/logout`,
          {
            userId,
            refreshToken,
          },
        ),
      );
    } catch (error) {
      this.logger.error(`Logout failed: ${error.message}`);
      // Logout should always succeed from the client's perspective
    }
  }

  async validateUser(userId: string): Promise<UserDto> {
    try {
      const response = await firstValueFrom(
        this.httpService.get<{ id: string; valid: boolean }>(
          `${this.coreServiceUrl}/api/auth/validate?userId=${userId}`,
        ),
      );

      if (!response.data.valid) {
        throw new UnauthorizedException('Invalid user');
      }

      return {
        id: response.data.id,
        email: '', // Would be populated by real implementation
      };
    } catch (error) {
      this.logger.error(`User validation failed: ${error.message}`);
      throw new UnauthorizedException('Invalid user');
    }
  }

  async handleOAuthLogin(
    provider: string,
    providerUserId: string,
    email: string,
    displayName: string,
    profileData: Record<string, any>,
  ): Promise<AuthResponseDto> {
    this.logger.log(`OAuth login attempt: ${provider} - ${email}`);

    try {
      const response = await firstValueFrom(
        this.httpService.post<SpringBootAuthResponse>(
          `${this.coreServiceUrl}/api/auth/oauth`,
          {
            provider,
            providerUserId,
            email,
            displayName: displayName || email,
            profileData,
          },
        ),
      );

      return this.mapToAuthResponseDto(response.data);
    } catch (error) {
      this.logger.error(`OAuth login failed: ${error.message}`);
      throw new BadRequestException('OAuth login failed');
    }
  }

  private mapToAuthResponseDto(response: SpringBootAuthResponse): AuthResponseDto {
    return {
      user: response.user,
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      expiresIn: response.expiresIn,
    };
  }
}

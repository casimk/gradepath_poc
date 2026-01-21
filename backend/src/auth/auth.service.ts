import { Injectable, BadRequestException, UnauthorizedException, Logger } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';

import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { AuthResponseDto } from './dto/auth-response.dto';

interface UserDto {
  id: string;
  username?: string;
  email: string;
  displayName?: string;
}

// Simple in-memory user store for testing
const users: Map<string, UserDto & { passwordHash: string }> = new Map();

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);
  private readonly refreshTokens: Map<string, { userId: string; expiresAt: number }> = new Map();

  constructor(
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
  ) {}

  async register(registerDto: RegisterDto): Promise<AuthResponseDto> {
    this.logger.log(`Registering new user: ${registerDto.username}`);

    // Check if username or email already exists
    for (const [id, user] of users.entries()) {
      if (user.username === registerDto.username || user.email === registerDto.email) {
        throw new BadRequestException('Username or email already exists');
      }
    }

    // Hash password
    const passwordHash = await bcrypt.hash(registerDto.password, 10);

    // Create user
    const userId = `user_${Date.now()}`;
    const user: UserDto & { passwordHash: string } = {
      id: userId,
      username: registerDto.username,
      email: registerDto.email,
      displayName: registerDto.displayName || registerDto.username,
      passwordHash,
    };

    users.set(userId, user);

    // Generate tokens
    return this.generateTokens(user);
  }

  async login(loginDto: LoginDto): Promise<AuthResponseDto> {
    this.logger.log(`Login attempt: ${loginDto.username || loginDto.email}`);

    // Find user by username or email
    let foundUser: UserDto & { passwordHash: string } | null = null;
    for (const [id, user] of users.entries()) {
      if (user.username === loginDto.username || user.email === loginDto.email) {
        foundUser = user;
        break;
      }
    }

    if (!foundUser) {
      throw new UnauthorizedException('Invalid credentials');
    }

    // Verify password
    const isValid = await bcrypt.compare(loginDto.password, foundUser.passwordHash);
    if (!isValid) {
      throw new UnauthorizedException('Invalid credentials');
    }

    // Generate tokens
    const { passwordHash, ...userDto } = foundUser;
    return this.generateTokens(userDto);
  }

  async refreshToken(refreshToken: string): Promise<AuthResponseDto> {
    this.logger.log('Refreshing token');

    const tokenData = this.refreshTokens.get(refreshToken);
    if (!tokenData) {
      throw new UnauthorizedException('Invalid refresh token');
    }

    const { userId, expiresAt } = tokenData;

    // Check if token is expired
    if (Date.now() > expiresAt) {
      this.refreshTokens.delete(refreshToken);
      throw new UnauthorizedException('Refresh token expired');
    }

    // Find user
    const user = users.get(userId);
    if (!user) {
      throw new UnauthorizedException('User not found');
    }

    // Revoke old refresh token
    this.refreshTokens.delete(refreshToken);

    const { passwordHash, ...userDto } = user;
    return this.generateTokens(userDto);
  }

  async logout(userId: string, refreshToken: string): Promise<void> {
    this.logger.log(`Logging out user: ${userId}`);

    // Revoke refresh token
    this.refreshTokens.delete(refreshToken);
  }

  async validateUser(userId: string): Promise<UserDto> {
    const user = users.get(userId);
    if (!user) {
      throw new UnauthorizedException('Invalid user');
    }

    const { passwordHash, ...userDto } = user;
    return userDto;
  }

  async handleOAuthLogin(
    provider: string,
    providerUserId: string,
    email: string,
    displayName: string,
    profileData: Record<string, any>,
  ): Promise<AuthResponseDto> {
    this.logger.log(`OAuth login attempt: ${provider} - ${email}`);

    // Check if user already exists by email
    let existingUser: UserDto & { passwordHash: string } | null = null;
    for (const [id, user] of users.entries()) {
      if (user.email === email) {
        existingUser = user;
        break;
      }
    }

    let user: UserDto & { passwordHash: string };

    if (existingUser) {
      user = existingUser;
    } else {
      // Create new user
      const userId = `user_${Date.now()}`;
      user = {
        id: userId,
        email,
        displayName: displayName || email,
        passwordHash: '', // OAuth users don't have password
      };
      users.set(userId, user);
    }

    const { passwordHash, ...userDto } = user;
    return this.generateTokens(userDto);
  }

  private generateTokens(user: UserDto): AuthResponseDto {
    const accessToken = this.jwtService.sign({
      sub: user.id,
      email: user.email,
      username: user.username,
      displayName: user.displayName,
    });

    const refreshToken = this.generateRefreshToken();
    const expiresIn = 900; // 15 minutes

    // Store refresh token
    this.refreshTokens.set(refreshToken, {
      userId: user.id,
      expiresAt: Date.now() + 7 * 24 * 60 * 60 * 1000, // 7 days
    });

    return {
      user,
      accessToken,
      refreshToken,
      expiresIn,
    };
  }

  private generateRefreshToken(): string {
    return `refresh_${Date.now()}_${Math.random().toString(36).substring(2, 15)}`;
  }
}
